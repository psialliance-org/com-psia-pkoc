import XCTest
@testable import PSIAExperienceApp

final class CredentialStoreTests: XCTestCase
{
    override func setUp()
    {
        super.setUp()
        CredentialStore.clear()
    }

    override func tearDown()
    {
        CredentialStore.clear()
        super.tearDown()
    }

    // MARK: - Save & Load

    func test_SaveAndLoad_roundTrip()
    {
        // Given a list of credential hex IDs
        let ids = ["aabbcc", "ddeeff", "112233"]

        // When we save and then load them
        CredentialStore.save(hexIds: ids)
        let loaded = CredentialStore.load()

        // Then all IDs should be present
        XCTAssertEqual(loaded, Set(ids))
    }

    func test_SaveAndLoad_singleItem()
    {
        // Given a single ID
        CredentialStore.save(hexIds: ["deadbeef"])

        // When we load
        let loaded = CredentialStore.load()

        // Then it should contain that single ID
        XCTAssertEqual(loaded, ["deadbeef"])
    }

    func test_Load_nothingSaved_returnsEmptySet()
    {
        // Given no prior save
        // When we load
        let loaded = CredentialStore.load()

        // Then the result should be empty
        XCTAssertTrue(loaded.isEmpty)
    }

    func test_Save_emptyList_loadReturnsEmpty()
    {
        // Given an empty list saved
        CredentialStore.save(hexIds: [])

        // When we load
        let loaded = CredentialStore.load()

        // Then the result should be empty
        XCTAssertTrue(loaded.isEmpty)
    }

    // MARK: - Clear

    func test_Clear_removesStoredData()
    {
        // Given previously saved credentials
        CredentialStore.save(hexIds: ["aabbcc"])

        // When we clear
        CredentialStore.clear()

        // Then load should return empty
        XCTAssertTrue(CredentialStore.load().isEmpty)
    }

    // MARK: - Overwrite

    func test_Save_overwritesPreviousData()
    {
        // Given previously saved credentials
        CredentialStore.save(hexIds: ["old-id"])

        // When we save new ones
        CredentialStore.save(hexIds: ["new-id-1", "new-id-2"])

        // Then load should return only the new IDs
        let loaded = CredentialStore.load()
        XCTAssertEqual(loaded, ["new-id-1", "new-id-2"])
        XCTAssertFalse(loaded.contains("old-id"))
    }

    // MARK: - Deduplication

    func test_Load_returnsDeduplicated()
    {
        // Given duplicate IDs saved via the comma-separated format
        CredentialStore.save(hexIds: ["aabb", "aabb", "ccdd"])

        // When we load (returns a Set)
        let loaded = CredentialStore.load()

        // Then duplicates should be collapsed
        XCTAssertEqual(loaded.count, 2)
        XCTAssertTrue(loaded.contains("aabb"))
        XCTAssertTrue(loaded.contains("ccdd"))
    }
}
