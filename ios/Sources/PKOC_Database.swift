import Foundation
import GRDB

public final class PKOCDatabase
{
    public let dbQueue: DatabaseQueue

    public struct Schema
    {
        public static let userVersion: Int32    = 2
        public static let applicationId: Int32  = 0x504B4F43
        public static let fileName: String      = "pkoc.sqlite"
        public static let bundledName: String   = "pkoc-baseline"
    }

    public init(filename: String = Schema.fileName) throws
    {
        let url = try Self.databaseURL(fileName: filename)
        try Self.ensureDatabaseFile(at: url)

        var config = Configuration()
        config.prepareDatabase
        { db in
            try db.execute(sql: "PRAGMA foreign_keys = ON;")
        }

        self.dbQueue = try DatabaseQueue(path: url.path, configuration: config)

        try Self.verifyOrBootstrapSchema(dbQueue: dbQueue)
    }
}

private extension PKOCDatabase
{
    static func supportDirectory() throws -> URL
    {
        let fm = FileManager.default
        let base = try fm.url(for: .applicationSupportDirectory,
                              in: .userDomainMask,
                              appropriateFor: nil,
                              create: true)
        let dir = base.appendingPathComponent("PKOC_Application", isDirectory: true)
        if !fm.fileExists(atPath: dir.path)
        {
            try fm.createDirectory(at: dir, withIntermediateDirectories: true)
        }
        return dir
    }

    static func databaseURL(fileName: String) throws -> URL
    {
        try supportDirectory().appendingPathComponent(fileName)
    }

    static func ensureDatabaseFile(at url: URL) throws
    {
        let fm = FileManager.default
        guard !fm.fileExists(atPath: url.path) else
        {
            return
        }

        if let bundled = Bundle.main.url(forResource: Schema.bundledName, withExtension: "sqlite")
        {
            try fm.copyItem(at: bundled, to: url)
            return
        }

        _ = try DatabaseQueue(path: url.path)
    }
}

private extension PKOCDatabase
{
    static func verifyOrBootstrapSchema(dbQueue: DatabaseQueue) throws
    {
        let (appId, userVer): (Int32, Int32) = try dbQueue.read
        { db in
            let id  = try Int32.fetchOne(db, sql: "PRAGMA application_id;") ?? 0
            let ver = try Int32.fetchOne(db, sql: "PRAGMA user_version;") ?? 0
            return (id, ver)
        }

        if appId == 0 && userVer == 0
        {
            try createBaselineSchema(dbQueue: dbQueue)
            return
        }

        guard appId == Schema.applicationId else
        {
            throw DatabaseError(message: "Unexpected application_id \(appId)")
        }

        guard userVer == Schema.userVersion else
        {
            #if DEBUG
            return
            #else
            throw DatabaseError(message: "Unsupported user_version \(userVer); expected \(Schema.userVersion)")
            #endif
        }
    }

    static func createBaselineSchema(dbQueue: DatabaseQueue) throws
    {
        try dbQueue.write
        { db in
            try db.execute(sql: "PRAGMA application_id = \(Schema.applicationId);")
            try db.execute(sql: "PRAGMA user_version = \(Schema.userVersion);")

            try db.create(table: "site")
            { t in
                t.column("siteId", .text).notNull().primaryKey()
                t.column("publicKeyHex", .text).notNull()
            }

            try db.create(table: "reader")
            { t in
                t.column("readerId", .text).notNull()
                t.column("siteId",   .text).notNull()
                t.primaryKey(["readerId", "siteId"])
                t.foreignKey(["siteId"], references: "site",
                             onDelete: .cascade, onUpdate: .cascade)
            }

            try db.execute(sql: "CREATE INDEX IF NOT EXISTS idx_reader_site ON reader(siteId);")
        }
    }
}
