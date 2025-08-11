import Foundation

struct FlowModel
{
    var connectionType : PKOC_ConnectionType
    var reader : ReaderModel = ReaderModel()
    var readerValid : Bool?
    var transientKeyPair : SecKey?
    var sharedSecret : [UInt8]?
    var status : ReaderUnlockStatus = ReaderUnlockStatus.Unknown
    var site : SiteModel?
    var counter : UInt32 = 1
}
