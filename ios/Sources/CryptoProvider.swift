import Foundation
import Combine
import LocalAuthentication
import CryptoKit
import CommonCrypto
import BigInt
import CryptoSwift

public class CryptoProvider : ObservableObject
{
    static private var publicKey : P256.Signing.PublicKey? = nil
    static private var privateKey : P256.Signing.PrivateKey? = nil
    static let IvPrepend = Data([0, 0, 0, 0, 0, 0, 0, 1])
    static let CcmTagLength = 128
    
    static func exportPublicKey() -> P256.Signing.PublicKey
    {
        AppLog.debug("Exporting public key", tag: "Crypto")
        return publicKey!
    }
    
    static func exportPrivateKey() -> P256.Signing.PrivateKey
    {
        AppLog.debug("Exporting private key", tag: "Crypto")
        return privateKey!
    }
    
    static func fromCompressedPublicKey(compressedPublicKey : [UInt8]) -> SecKey?
    {
        guard compressedPublicKey.count == 33 else
        {
            AppLog.error("Invalid compressed public key length: \(compressedPublicKey.count)", tag: "Crypto")
            return nil
        }
        
        let prefix : UInt8 = compressedPublicKey[0]
        guard prefix == 0x02 || prefix == 0x03 else
        {
            AppLog.error("Invalid compressed public key prefix: \(prefix)", tag: "Crypto")
            return nil
        }
        
        let xArray : [UInt8] = Array(compressedPublicKey[1...])
        let x = BigUInt(Data(xArray))
        let p = BigUInt(Data(hex: "ffffffff00000001000000000000000000000000ffffffffffffffffffffffff")!)
        let a = BigUInt(Data(hex: "ffffffff00000001000000000000000000000000fffffffffffffffffffffffc")!)
        let b = BigUInt(Data(hex: "5ac635d8aa3a93e7b3ebbd55769886bc651d06b0cc53b0f63bce3c3e27d2604b")!)
        
        var y = (x.power(3) + (a * x) + b).power((p + 1) / 4, modulus: p)
        var yArray = Array(y.serialize())
        
        if ((prefix == 2 && y % 2 != 0) || (prefix == 3 && y % 2 == 0))
        {
            y = (p - y) % p
        }
        
        yArray = Array(y.serialize())
        
        if (yArray.count < 32)
        {
            yArray = [UInt8](repeating: 0, count: 32 - yArray.count) + yArray
            AppLog.warn("Right padding yArray with zeros", tag: "Crypto")
        }
        
        let publicKey = [0x04] + xArray + yArray
        
        let attributes: [String: Any] =
        [
            kSecAttrKeyClass as String: kSecAttrKeyClassPublic,
            kSecAttrType as String: kSecAttrKeyTypeEC,
            kSecAttrKeySizeInBits as String: 256,
        ]
        
        var error: Unmanaged<CFError>?
        let secKey = SecKeyCreateWithData(Data(publicKey) as CFData, attributes as CFDictionary, &error)
        
        if let error = error
        {
            AppLog.error("Failed to create SecKey: \(error)", tag: "Crypto")
        }
        else
        {
            AppLog.info("Successfully decompressed public key", tag: "Crypto")
        }
        
        return secKey
    }
    
    static func signNonceWithPrivateKey(nonce: Data) -> [UInt8]
    {
        AppLog.debug("Signing nonce of size \(nonce.count)", tag: "Crypto", payload: nonce)
        let signature = try! privateKey!.signature(for: nonce)
        return [UInt8](signature.rawRepresentation)
    }
    
    static func verifySignedMessage(message: [UInt8], signature : [UInt8], publicKey : [UInt8]) -> Bool
    {
        do
        {
            let publicKeyP265 = try P256.Signing.PublicKey(x963Representation: publicKey)
            let ecdsaSignature = try P256.Signing.ECDSASignature(rawRepresentation: signature)
            let fileDataDigest = SHA256.hash(data: message)
            
            let result = publicKeyP265.isValidSignature(ecdsaSignature, for: fileDataDigest)
            AppLog.info("Signature verification result: \(result)", tag: "Crypto")
            return result
        }
        catch
        {
            AppLog.error("Signature verification failed: \(error)", tag: "Crypto")
            return false
        }
    }
    
    static func loadKeys(privateKey: P256.Signing.PrivateKey, publicKey: P256.Signing.PublicKey)
    {
        AppLog.info("Loading keys into CryptoProvider", tag: "Crypto")
        self.publicKey = publicKey
        self.privateKey = privateKey
    }
    
    static func generateAndSendPublishKey(_ completionHandler: @escaping (Bool) -> Void)
    {
        AppLog.info("Generating new signing keypair", tag: "Crypto")
        let signingKey = P256.Signing.PrivateKey()
        self.privateKey = signingKey
        self.publicKey = signingKey.publicKey
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
        let key = SecKeyCreateRandomKey(attributes as CFDictionary, &error)!
        
        if let error = error
        {
            AppLog.error("Transient key generation error: \(error)", tag: "Crypto")
        }
        else
        {
            AppLog.info("Generated transient key", tag: "Crypto")
        }
        
        return key
    }
    
    static func getCcmIv(counter: UInt32) -> [UInt8]
    {
        var counterBigEndian = counter.bigEndian
        let counterBytes = withUnsafeBytes(of: &counterBigEndian)
        {
            Array($0)
        }
        
        let iv: [UInt8] = Array(IvPrepend) + counterBytes
        AppLog.debug("Generated CCM IV for counter \(counter)", tag: "Crypto", payload: Data(iv))
        return iv
    }
    
    static func getAES256(secretKey : [UInt8], data : [UInt8], counter : UInt32) -> [UInt8]?
    {
        let iv = getCcmIv(counter: counter)
        AppLog.debug("Encrypting with AES-CCM, key size=\(secretKey.count), data size=\(data.count)", tag: "Crypto")
        
        let aes = try? AES(
            key: secretKey,
            blockMode: CCM(iv: iv, tagLength: CcmTagLength / 8, messageLength: data.count),
            padding: .noPadding
        )
        
        let encrypted = try? aes?.encrypt(data)
        
        if encrypted == nil
        {
            AppLog.error("AES-CCM encryption failed", tag: "Crypto")
        }
        
        return encrypted
    }
    
    static func createSharedSecret(privateKey : SecKey, publicKey : SecKey) -> Data?
    {
        var error: Unmanaged<CFError>?
        let keyPairAttr : [String : Any] =
        [
            kSecAttrKeySizeInBits as String: 256,
            SecKeyKeyExchangeParameter.requestedSize.rawValue as String: 32,
            kSecAttrKeyType as String: kSecAttrKeyTypeEC,
            kSecPrivateKeyAttrs as String: [kSecAttrIsPermanent as String: false],
            kSecPublicKeyAttrs as String: [kSecAttrIsPermanent as String: false]
        ]
        
        let algorithm = SecKeyAlgorithm.ecdhKeyExchangeStandard
        let shared = SecKeyCopyKeyExchangeResult(privateKey, algorithm, publicKey, keyPairAttr as CFDictionary, &error) as Data?
        
        if let error = error
        {
            AppLog.error("Shared secret creation error: \(error)", tag: "Crypto")
        }
        else
        {
            AppLog.info("Shared secret created", tag: "Crypto")
        }
        
        return shared
    }
    
    static func deriveAesKeyFromSharedSecretSimple(_ sharedSecret: [UInt8]) -> [UInt8]
    {
        AppLog.debug("Deriving AES key from shared secret", tag: "Crypto", payload: Data(sharedSecret))
        let digest = SHA256.hash(data: Data(sharedSecret))
        return Array(digest)
    }
}
