import Foundation

enum PKOC_ConnectionType: Int, CaseIterable, Equatable
{
    case Uncompressed = 0,
         ECDHE_Full = 1
}
