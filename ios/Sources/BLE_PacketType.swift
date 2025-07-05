enum BLE_PacketType : UInt8
{
    case Void = 0x00
    case PublicKey = 0x01
    case CompressedEphemeralPublicKey = 0x02
    case DigitalSignature = 0x03
    case Response = 0x04
    case UncompressedEphemeralPublicKey = 0x07
    case LastUpdateTime = 0x09
    case ProtocolVersion = 0x0C
    case ReaderLocationIdentifier = 0x0D
    case SiteIdenifier = 0x0E
    case EncryptedDataFollows = 0x40
    case ManufacturerSpecificData = 0x80
    case ErrorPacket = 0x99
}
