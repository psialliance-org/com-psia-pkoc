import Foundation

public struct PKOCRepositories
{
    public let siteRepo  : SiteRepository
    public let readerRepo: ReaderRepository

    public static func make() throws -> PKOCRepositories
    {
        let db = try PKOCDatabase()
        return .init(siteRepo: GRDBSiteRepository(db: db),
                     readerRepo: GRDBReaderRepository(db: db))
    }

    public static func makeEphemeralForTests() throws -> PKOCRepositories
    {
        let tmpDir = FileManager.default.temporaryDirectory
        let dbURL  = tmpDir.appendingPathComponent("pkoc-test-\(UUID().uuidString).sqlite")
        let db     = try PKOCDatabase(filename: dbURL.lastPathComponent)
        return .init(siteRepo: GRDBSiteRepository(db: db),
                     readerRepo: GRDBReaderRepository(db: db))
    }
}

public enum Seed
{
    public static let siteA = Site(
        siteId: UUID(uuidString: "b9897ed0-5272-4341-979a-b69850112d80")!,
        publicKeyHex: "04b71bb4b0de53f06a09ea6c91b483a898645005a30ec9422b95a67908f640abac440b1e4e705db4a626f7ac4e4dcfeba9f7157872446e61f58282c426f4e838af"
    )
    public static let readerA = Reader(
        readerId: UUID(uuidString: "ad0cbc8f-c353-427a-b479-37b5efcff6be")!,
        siteId: UUID(uuidString: "b9897ed0-5272-4341-979a-b69850112d80")!
    )
}

public enum RepoTools
{
    public static func seedIfEmpty(siteRepo: SiteRepository,
                                   readerRepo: ReaderRepository,
                                   seedSites: [Site],
                                   seedReaders: [Reader]) async throws
    {
        if try await siteRepo.list().isEmpty
        {
            for s in seedSites   { try await siteRepo.upsert(s) }
            for r in seedReaders { try await readerRepo.upsert(r) }
        }
    }
}
