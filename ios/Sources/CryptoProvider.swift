import Foundation
import Combine
import LocalAuthentication
import CryptoKit
import CommonCrypto
import BigInt
import CryptoSwift

public class CryptoProvider: ObservableObject
{
    static private var publicKey : P256.Signing.PublicKey? = nil
    static private var privateKey : P256.Signing.PrivateKey? = nil
    static let IvPrepend = Data([0, 0, 0, 0, 0, 0, 0, 1])
    static let CcmTagLength = 128
    
    static func exportPublicKey() -> P256.Signing.PublicKey
    {
        return publicKey!
    }
    
    static func exportPrivateKey() -> P256.Signing.PrivateKey
    {
        return privateKey!
    }
    
    static func fromCompressedPublicKey(compressedPublicKey : [UInt8]) -> SecKey?
    {
        if (compressedPublicKey.count != 33)
        {
            print ("Invalid compressed public key -- unexpected length")
            return nil
        }
        
        let prefix : UInt8 = compressedPublicKey[0]
        
        if (prefix != 0x02 && prefix != 0x03)
        {
            print ("Invalid compressed public key -- unexpected first byte")
            return nil
        }
        
        let xArray : [UInt8] = Array(compressedPublicKey[1...])

        let x = BigUInt(Data(xArray))
        let p = BigUInt(Data(hex: "ffffffff00000001000000000000000000000000ffffffffffffffffffffffff")!)
        let a = BigUInt(Data(hex: "ffffffff00000001000000000000000000000000fffffffffffffffffffffffc")!)
        let b = BigUInt(Data(hex: "5ac635d8aa3a93e7b3ebbd55769886bc651d06b0cc53b0f63bce3c3e27d2604b")!)
        let ySq = (x.power(3) + (a * x) + b)
        var y = ySq.power((p + 1)/4, modulus: p)
        
        var yArray = Array(y.serialize())

        if ((prefix == 2 && y.power(BigUInt(1), modulus: 2) != 0) || (prefix == 3 && y.power(BigUInt(1), modulus: 2)  == 0))
        {
            y = (p - y).power(BigUInt(1), modulus: p)
        }

        yArray = Array(y.serialize())
        
        if (yArray.count < 32)
        {
            yArray = [UInt8](repeating:0,count:32-yArray.count) + yArray
            print("right padding yArray with zeros")
        }
        
        let publicKey = [0x04] + xArray + yArray
        
        let attributes: [String: Any] =
        [
            kSecAttrKeyClass as String: kSecAttrKeyClassPublic,
            kSecAttrType as String: kSecAttrKeyTypeEC,
            kSecAttrKeySizeInBits as String: 256,
        ]

        var error: Unmanaged<CFError>?
        return SecKeyCreateWithData(Data(publicKey) as CFData, attributes as CFDictionary, &error)
    }
    
    static func signNonceWithPrivateKey(nonce: Data) -> [UInt8]
    {
        let signature = try! privateKey!.signature(for: nonce)
        return [UInt8](signature.rawRepresentation)
    }
    
    static func verifySignedMessage(message: [UInt8], signature : [UInt8], publicKey : [UInt8]) -> Bool
    {
        let publicKeyP265 = try! P256.Signing.PublicKey(x963Representation: publicKey)
        let ecdsaSignature = try! P256.Signing.ECDSASignature(rawRepresentation: signature)
        let fileDataDigest = SHA256.hash(data: message)
        
        let result = publicKeyP265.isValidSignature(ecdsaSignature, for: fileDataDigest)
        return result
    }
    
    static func loadKeys(privateKey: P256.Signing.PrivateKey, publicKey: P256.Signing.PublicKey)
    {
        self.publicKey = publicKey
        self.privateKey = privateKey
    }
    
    static func generateAndSendPublishKey(_ completionHandler: @escaping (Bool) -> Void)
    {
        let signingKey = P256.Signing.PrivateKey()
        self.privateKey = signingKey
        
        let signingPublicKey = signingKey.publicKey
        self.publicKey = signingPublicKey
        completionHandler(true)
    }
    
    static func generateTransientKey() -> SecKey
    {
        let attributes: [String: Any] =
        [
            kSecAttrType as String: kSecAttrKeyTypeEC,
            kSecAttrKeySizeInBits as String: 256,
            kSecPrivateKeyAttrs as String:
                [
                    kSecAttrIsPermanent as String: true
                ]
        ]
        
        var error: Unmanaged<CFError>?
        return SecKeyCreateRandomKey(attributes as CFDictionary, &error)!
    }
    
    static func getCcmIv(counter: UInt32) -> [UInt8]
    {
        var counterBigEndian = counter.bigEndian
        let counterBytes = withUnsafeBytes(of: &counterBigEndian)
        {
            Array($0)
        }
        return IvPrepend + counterBytes
    }
    
    static func getAES256(secretKey : [UInt8], data : [UInt8], counter : UInt32) -> [UInt8]?
    {
        let iv = getCcmIv(counter: counter)
        
        let aes = try? AES(key: secretKey, blockMode: CCM(iv: iv, tagLength: CcmTagLength / 8, messageLength: data.count), padding: .noPadding)
        
        return try? (aes?.encrypt(data))
    }
    
    static func createSharedSecret(privateKey : SecKey, publicKey : SecKey) -> Data?
    {
        var error: Unmanaged<CFError>?
        
        let keyPairAttr:[String : Any] =
        [
            kSecAttrKeySizeInBits as String: 256,
            SecKeyKeyExchangeParameter.requestedSize.rawValue as String: 32,
            kSecAttrKeyType as String: kSecAttrKeyTypeEC,
            kSecPrivateKeyAttrs as String: [kSecAttrIsPermanent as String: false],
            kSecPublicKeyAttrs as String:[kSecAttrIsPermanent as String: false]
        ]

        let algorithm = SecKeyAlgorithm.ecdhKeyExchangeStandard
        let shared = SecKeyCopyKeyExchangeResult(privateKey, algorithm, publicKey, keyPairAttr as CFDictionary, &error) as Data?
        return shared
    }
}
