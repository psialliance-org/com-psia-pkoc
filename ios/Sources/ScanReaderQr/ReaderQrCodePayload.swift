struct ReaderQrCodePayload : Decodable
{
    let siteUuid: String
    let readerUuid: String
    let publicKey: String
}
