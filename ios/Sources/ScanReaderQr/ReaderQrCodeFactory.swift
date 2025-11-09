import Foundation

struct ReaderQrCodeFactory
{
    static func build(from json: String) throws -> (ReaderModel, SiteModel)
    {
        let data = Data(json.utf8)
        let decoder = JSONDecoder()
        let payload: ReaderQrCodePayload

        do
        {
            payload = try decoder.decode(ReaderQrCodePayload.self, from: data)
        }
        catch
        {
            throw ReaderQrCodeError.invalidJSON
        }

        guard
            let siteUUID = UUID(uuidString: payload.siteUuid),
            let readerUUID = UUID(uuidString: payload.readerUuid)
        else
        {
            throw ReaderQrCodeError.invalidUUID
        }

        guard let publicKeyBytes = payload.publicKey.hexStringToBytes()
        else
        {
            throw ReaderQrCodeError.invalidPublicKeyEncoding
        }

        let siteIdentifierBytes = siteUUID.asUInt8Array()
        let readerIdentifierBytes = readerUUID.asUInt8Array()

        let siteModel = SiteModel(
            SiteIdentifier: siteIdentifierBytes,
            PublicKey: publicKeyBytes
        )

        let readerModel = ReaderModel(
            ProtocolVersion: nil,
            ReaderEphemeralPublicKey: nil,
            ReaderIdentifier: readerIdentifierBytes,
            SiteIdentifier: siteIdentifierBytes
        )

        return (readerModel, siteModel)
    }

    static func importToDatabase(
        readerModel: ReaderModel,
        siteModel: SiteModel
    ) async throws
    {
        guard let siteId = UUID(bytes: siteModel.SiteIdentifier)
        else
        {
            throw ReaderQrCodeError.invalidUUID
        }

        guard
            let readerIdBytes = readerModel.ReaderIdentifier,
            let readerId = UUID(bytes: readerIdBytes)
        else
        {
            throw ReaderQrCodeError.invalidUUID
        }

        let publicKeyHex = Data(siteModel.PublicKey).hexadecimal()

        let site = Site(
            siteId: siteId,
            publicKeyHex: publicKeyHex
        )

        let reader = Reader(
            readerId: readerId,
            siteId: siteId
        )

        let repos = try PKOCRepositories.make()
        try await repos.siteRepo.upsert(site)
        try await repos.readerRepo.upsert(reader)
    }

    @discardableResult
    static func importFromQRCodeJSON(_ json: String) async throws -> (ReaderModel, SiteModel)
    {
        let (readerModel, siteModel) = try build(from: json)
        try await importToDatabase(readerModel: readerModel, siteModel: siteModel)
        return (readerModel, siteModel)
    }
}
