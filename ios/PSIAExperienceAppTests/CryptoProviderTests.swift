import XCTest
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
}
