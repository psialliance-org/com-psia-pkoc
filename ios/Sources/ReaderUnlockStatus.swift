import Foundation

enum ReaderUnlockStatus : UInt8, CaseIterable, Equatable
{
    case Unknown = 0
    case CompletedTransaction = 1
    case AccessDenied = 2
    case AccessGranted  = 3
    case DecryptionErrorGcm = 4
    case InvalidSecurityStatus = 5
    case SignatureInvalid = 6
    case DecryptionErrorCcm = 7
    case Unrecognized = 8
}
