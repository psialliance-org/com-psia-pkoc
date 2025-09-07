import Foundation
import GRDB

public struct Site: Codable, Equatable, Sendable
{
    public var siteId: UUID
    public var publicKeyHex: String

    public init(siteId: UUID, publicKeyHex: String)
    {
        self.siteId = siteId
        self.publicKeyHex = publicKeyHex
    }
}

public struct Reader: Codable, Equatable, Sendable
{
    public var readerId: UUID
    public var siteId: UUID

    public init(readerId: UUID, siteId: UUID)
    {
        self.readerId = readerId
        self.siteId = siteId
    }
}

public enum RepoError: Error, Sendable
{
    case validationFailed(String, Data?)
    case notFound(String)
    case foreignKeyViolation(String)
    case conflict(String)
    case persistenceError(String, Error?)
}

public enum Validator
{
    public static func isUncompressed65ByteHex(_ hex: String) -> Bool
    {
        guard hex.count == 130, hex.hasPrefix("04") else { return false }
        let set = CharacterSet(charactersIn: "0123456789abcdefABCDEF")
        return hex.allSatisfy { String($0).rangeOfCharacter(from: set) != nil }
    }
}

public protocol SiteRepository
{
    func upsert(_ site: Site) async throws
    func get(_ siteId: UUID) async throws -> Site?
    func list() async throws -> [Site]
    func delete(_ siteId: UUID) async throws
    func changes() -> AsyncStream<[Site]>
}

public protocol ReaderRepository
{
    func upsert(_ reader: Reader) async throws
    func get(readerId: UUID, siteId: UUID) async throws -> Reader?
    func listBySite(_ siteId: UUID) async throws -> [Reader]
    func listAll() async throws -> [Reader]
    func delete(readerId: UUID, siteId: UUID) async throws
    func changes() -> AsyncStream<[Reader]>
}

final class ChangeBus
{
    private var siteSinks   = [AsyncStream<[Site]>.Continuation]()
    private var readerSinks = [AsyncStream<[Reader]>.Continuation]()
    private let lock = NSLock()

    func sites(initial: @escaping @Sendable () async -> [Site]) -> AsyncStream<[Site]>
    {
        AsyncStream
        { cont in
            lock.lock()
            siteSinks.append(cont)
            lock.unlock()

            Task
            {
                cont.yield(await initial())
            }
        }
    }

    func readers(initial: @escaping @Sendable () async -> [Reader]) -> AsyncStream<[Reader]>
    {
        AsyncStream
        { cont in
            lock.lock()
            readerSinks.append(cont)
            lock.unlock()

            Task
            {
                cont.yield(await initial())
            }
        }
    }

    func emitSites(_ value: [Site])
    {
        lock.lock()
        let sinks = siteSinks
        lock.unlock()

        for c in sinks
        {
            c.yield(value)
        }
    }

    func emitReaders(_ value: [Reader])
    {
        lock.lock()
        let sinks = readerSinks
        lock.unlock()

        for c in sinks
        {
            c.yield(value)
        }
    }
}

private let bus = ChangeBus()

@inline(__always)
private func uuidLower(_ id: UUID) -> String
{
    id.uuidString.lowercased()
}

public final class GRDBSiteRepository : SiteRepository
{
    private let db: PKOCDatabase

    public init(db: PKOCDatabase)
    {
        self.db = db
    }

    public func upsert(_ site: Site) async throws
    {
        guard Validator.isUncompressed65ByteHex(site.publicKeyHex)
        else
        {
            throw RepoError.validationFailed("publicKeyHex invalid (expect 65-byte uncompressed hex starting with 04).", Data(hex: site.publicKeyHex))
        }

        do
        {
            try await db.dbQueue.write
            { db in
                try db.execute(
                    sql: """
                         INSERT INTO site(siteId, publicKeyHex)
                         VALUES(?, ?)
                         ON CONFLICT(siteId) DO UPDATE SET publicKeyHex = excluded.publicKeyHex;
                         """,
                    arguments: [uuidLower(site.siteId), site.publicKeyHex]
                )
            }
        }
        catch
        {
            throw RepoError.persistenceError("Upsert Site failed", error)
        }

        if let snapshot = try? await list()
        {
            bus.emitSites(snapshot)
        }
    }

    public func get(_ siteId: UUID) async throws -> Site?
    {
        try await db.dbQueue.read
        { db in
            try Row.fetchOne(db,
                             sql: "SELECT * FROM site WHERE siteId = ?",
                             arguments: [uuidLower(siteId)])
            .map
            {
                Site(siteId: UUID(uuidString: $0["siteId"])!, publicKeyHex: $0["publicKeyHex"])
            }
        }
    }

    public func list() async throws -> [Site]
    {
        try await db.dbQueue.read
        { db in
            try Row.fetchAll(db,
                             sql: "SELECT * FROM site ORDER BY siteId")
            .map
            {
                Site(siteId: UUID(uuidString: $0["siteId"])!, publicKeyHex: $0["publicKeyHex"])
            }
        }
    }

    public func delete(_ siteId: UUID) async throws
    {
        do
        {
            try await db.dbQueue.write { db in
                _ = try db.execute(
                    sql: "DELETE FROM site WHERE siteId = ?",
                    arguments: [siteId.dbString]
                )
            }
        }
        catch
        {
            throw RepoError.persistenceError("Delete Site failed", error)
        }

        if let sites = try? await list() {
            bus.emitSites(sites)
        }
        if let readers = try? await GRDBReaderRepository(db: db).listAll() {
            bus.emitReaders(readers)
        }
    }

    public func changes() -> AsyncStream<[Site]>
    {
        bus.sites { [weak self] in
            (try? await self?.list()) ?? []
        }
    }
}

public final class GRDBReaderRepository : ReaderRepository
{
    private let db: PKOCDatabase

    public init(db: PKOCDatabase)
    {
        self.db = db
    }

    public func upsert(_ reader: Reader) async throws
    {
        let siteExists: Bool = try await db.dbQueue.read
        { db in
            try String.fetchOne(db,
                                sql: "SELECT siteId FROM site WHERE siteId = ?",
                                arguments: [uuidLower(reader.siteId)]) != nil
        }

        if !siteExists
        {
            throw RepoError.foreignKeyViolation("Reader references non-existent siteId.")
        }

        do
        {
            try await db.dbQueue.write
            { db in
                try db.execute(
                    sql: """
                         INSERT INTO reader(readerId, siteId)
                         VALUES(?, ?)
                         ON CONFLICT(readerId, siteId) DO NOTHING;
                         """,
                    arguments: [uuidLower(reader.readerId), uuidLower(reader.siteId)]
                )
            }
        }
        catch
        {
            throw RepoError.persistenceError("Upsert Reader failed", error)
        }

        if let readers = try? await listAll()
        {
            bus.emitReaders(readers)
        }
    }

    public func get(readerId: UUID, siteId: UUID) async throws -> Reader?
    {
        try await db.dbQueue.read
        { db in
            try Row.fetchOne(db,
                             sql: "SELECT * FROM reader WHERE readerId = ? AND siteId = ?",
                             arguments: [uuidLower(readerId), uuidLower(siteId)])
            .map
            {
                Reader(readerId: UUID(uuidString: $0["readerId"])!,
                       siteId:   UUID(uuidString: $0["siteId"])!)
            }
        }
    }

    public func listBySite(_ siteId: UUID) async throws -> [Reader]
    {
        try await db.dbQueue.read
        { db in
            try Row.fetchAll(db,
                             sql: "SELECT * FROM reader WHERE siteId = ? ORDER BY readerId",
                             arguments: [uuidLower(siteId)])
            .map
            {
                Reader(readerId: UUID(uuidString: $0["readerId"])!,
                       siteId:   UUID(uuidString: $0["siteId"])!)
            }
        }
    }

    public func listAll() async throws -> [Reader]
    {
        try await db.dbQueue.read
        { db in
            try Row.fetchAll(db,
                             sql: "SELECT * FROM reader ORDER BY siteId, readerId")
            .map
            {
                Reader(readerId: UUID(uuidString: $0["readerId"])!,
                       siteId:   UUID(uuidString: $0["siteId"])!)
            }
        }
    }

    public func delete(readerId: UUID, siteId: UUID) async throws
    {
        do
        {
            try await db.dbQueue.write
            { db in
                _ = try db.execute(sql: "DELETE FROM reader WHERE readerId = ? AND siteId = ?",
                                   arguments: [uuidLower(readerId), uuidLower(siteId)])
            }
        }
        catch
        {
            throw RepoError.persistenceError("Delete Reader failed", error)
        }

        if let readers = try? await listAll()
        {
            bus.emitReaders(readers)
        }
    }

    public func changes() -> AsyncStream<[Reader]>
    {
        bus.readers { [weak self] in
            (try? await self?.listAll()) ?? []
        }
    }
}
