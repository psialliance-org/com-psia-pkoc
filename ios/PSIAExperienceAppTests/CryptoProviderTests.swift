import XCTest
import CryptoSwift
@testable import PSIAExperienceApp

final class PKOCIVTests: XCTestCase
{
    private let expectedPrefix: [UInt8] = [0,0,0,0,0,0,0,1]
    
    func test_IV_is12Bytes()
    {
        let iv = CryptoProvider.getCcmIv(counter: 1)
        XCTAssertEqual(iv.count, 12, "IV must be exactly 12 bytes (8 prefix + 4 counter).")
    }
    
    func test_IV_hasCorrectPrefix()
    {
        let iv = CryptoProvider.getCcmIv(counter: 1)
        XCTAssertEqual(Array(iv.prefix(8)), expectedPrefix, "First 8 bytes must match the fixed prefix.")
    }
    
    func test_Counter_isBigEndian_encodedCorrectly_forOne()
    {
        let iv = CryptoProvider.getCcmIv(counter: 1)
        let counterBytes = Array(iv.suffix(4))
        XCTAssertEqual(counterBytes, [0x00, 0x00, 0x00, 0x01], "Counter=1 must encode to 00 00 00 01 (big-endian).")
    }
    
    func test_Counter_isBigEndian_encodedCorrectly_forArbitraryValue()
    {
        // 0x12345678 → 12 34 56 78 (big-endian)
        let iv = CryptoProvider.getCcmIv(counter: 0x12345678)
        let counterBytes = Array(iv.suffix(4))
        XCTAssertEqual(counterBytes, [0x12, 0x34, 0x56, 0x78], "Counter must be big-endian encoded.")
    }
    
    func test_Counter_maxValue()
    {
        // 0xFFFFFFFF → FF FF FF FF (big-endian)
        let iv = CryptoProvider.getCcmIv(counter: 0xFFFF_FFFF)
        let counterBytes = Array(iv.suffix(4))
        XCTAssertEqual(counterBytes, [0xFF, 0xFF, 0xFF, 0xFF], "Max UInt32 must encode to FF FF FF FF.")
    }
    
    func test_DifferentCounters_produceDifferentIVs()
    {
        let iv1 = CryptoProvider.getCcmIv(counter: 1)
        let iv2 = CryptoProvider.getCcmIv(counter: 2)
        XCTAssertNotEqual(iv1, iv2, "Different counters must yield different IVs.")
    }
    
    func test_PrefixConstant_notMutatedAcrossCalls()
    {
        let iv1 = CryptoProvider.getCcmIv(counter: 1)
        let iv2 = CryptoProvider.getCcmIv(counter: 2)
        XCTAssertEqual(Array(iv1.prefix(8)), expectedPrefix, "Prefix must remain constant.")
        XCTAssertEqual(Array(iv2.prefix(8)), expectedPrefix, "Prefix must remain constant.")
    }
    
    private func decryptRef(
            key: [UInt8],
            iv: [UInt8],
            ciphertext: [UInt8],
            tagBytes: Int,
            messageLength: Int
        ) throws -> [UInt8]
        {
            let aes = try AES(
                key: key,
                blockMode: CCM(iv: iv, tagLength: tagBytes, messageLength: messageLength),
                padding: .noPadding
            )
            return try aes.decrypt(ciphertext)
        }

        // MARK: - Tests

        func test_OutputLength_Includes16ByteTag()
        {
            let key = [UInt8](repeating: 0xAB, count: 32)
            let msg = [UInt8](repeating: 0xCD, count: 25)
            let tagBytes = CryptoProvider.CcmTagLength / 8

            XCTAssertEqual(tagBytes, 16, "CcmTagLength=128 → 16-byte tag")

            guard let ct = CryptoProvider.getAES256(secretKey: key, data: msg, counter: 7)
            else
            {
                XCTFail("Encryption returned nil")
                return
            }

            XCTAssertEqual(ct.count, msg.count + tagBytes,
                           "Ciphertext length must be plaintext + tag")
        }

        func test_EncryptThenDecrypt_RoundTrip()
        {
            var key = [UInt8](repeating: 0, count: 32)
            for i in 0..<key.count
            {
                key[i] = UInt8(0x20 + i)
            }

            let message = Array("PKOC-CCM iOS roundtrip".utf8)
            let counter: UInt32 = 123
            let tagBytes = CryptoProvider.CcmTagLength / 8
            let iv = CryptoProvider.getCcmIv(counter: counter)

            guard let ciphertext = CryptoProvider.getAES256(secretKey: key, data: message, counter: counter)
            else
            {
                XCTFail("Encryption returned nil")
                return
            }

            XCTAssertEqual(ciphertext.count, message.count + tagBytes)

            do
            {
                let plaintext = try decryptRef(
                    key: key,
                    iv: iv,
                    ciphertext: ciphertext,
                    tagBytes: tagBytes,
                    messageLength: message.count
                )
                XCTAssertEqual(plaintext, message)
            }
            catch
            {
                XCTFail("Reference decryption failed: \(error)")
            }
        }

        func test_Deterministic_ForSameInputs()
        {
            let key = [UInt8](repeating: 0x11, count: 32)
            let message = Array("Determinism".utf8)
            let counter: UInt32 = 42

            let c1 = CryptoProvider.getAES256(secretKey: key, data: message, counter: counter)
            let c2 = CryptoProvider.getAES256(secretKey: key, data: message, counter: counter)

            XCTAssertNotNil(c1)
            XCTAssertNotNil(c2)
            XCTAssertEqual(c1!, c2!, "Same key/IV/message should yield identical ciphertext")
        }

        func test_DifferentCounters_ProduceDifferentCiphertext()
        {
            let key = [UInt8](repeating: 0x5A, count: 32)
            let message = Array("nonce matters".utf8)

            let c1 = CryptoProvider.getAES256(secretKey: key, data: message, counter: 1)
            let c2 = CryptoProvider.getAES256(secretKey: key, data: message, counter: 2)

            XCTAssertNotNil(c1)
            XCTAssertNotNil(c2)
            XCTAssertNotEqual(c1!, c2!, "Changing the counter (IV) must change the ciphertext")
        }

        func test_EmptyMessage_ReturnsOnlyTag()
        {
            let key = [UInt8](repeating: 0xAA, count: 32)
            let message: [UInt8] = []
            let counter: UInt32 = 7
            let tagBytes = CryptoProvider.CcmTagLength / 8

            guard let ct = CryptoProvider.getAES256(secretKey: key, data: message, counter: counter)
            else
            {
                XCTFail("Encryption returned nil for empty message")
                return
            }

            XCTAssertEqual(ct.count, tagBytes, "Empty message should yield only the tag")
        }

        func test_InvalidKeyLength_ReturnsNil()
        {
            // 31 bytes is invalid for AES
            let badKey = [UInt8](repeating: 0x00, count: 31)
            let message = Array("abc".utf8)

            let ct = CryptoProvider.getAES256(secretKey: badKey, data: message, counter: 1)
            XCTAssertNil(ct, "Invalid AES key length should cause getAES256 to return nil")
        }

        func test_TamperResistance_RefDecryptFailsOnBitFlip()
        {
            let key = [UInt8](repeating: 0x44, count: 32)
            let message = Array("integrity check".utf8)
            let counter: UInt32 = 99
            let tagBytes = CryptoProvider.CcmTagLength / 8
            let iv = CryptoProvider.getCcmIv(counter: counter)

            guard var ct = CryptoProvider.getAES256(secretKey: key, data: message, counter: counter)
            else
            {
                XCTFail("Encryption returned nil")
                return
            }

            // Flip a bit in the ciphertext; reference decrypt should throw
            ct[0] ^= 0x01

            XCTAssertThrowsError(
                try decryptRef(key: key, iv: iv, ciphertext: ct, tagBytes: tagBytes, messageLength: message.count),
                "Tampered ciphertext should fail CCM authentication"
            )
        }
}
