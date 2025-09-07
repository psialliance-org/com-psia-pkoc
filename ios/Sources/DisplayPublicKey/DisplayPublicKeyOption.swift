import Foundation

enum DisplayPublicKeyOption : Int, CaseIterable, Equatable
{
    case FullPublicKey = 0,
        Bit64 = 1,
        Bit128 = 2,
        Bit256 = 3
}
