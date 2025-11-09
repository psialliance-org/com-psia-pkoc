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
