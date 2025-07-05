import Foundation

struct ReaderModel
{
    var ProtocolVersion : [UInt8]?
    var ReaderEphemeralPublicKey : [UInt8]?
    var ReaderIdentifier : [UInt8]?
    var SiteIdentifier : [UInt8]?
}
