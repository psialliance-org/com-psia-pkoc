public enum ProtocolVersion : UInt8
{
    case v1 = 0x01
}

public struct FeatureBits : OptionSet
{
    public let rawValue: UInt16
    public init(rawValue: UInt16) { self.rawValue = rawValue }

    public static let ccm = PKOCFeatureBits(rawValue: 0x0001)
    public static let gcm = PKOCFeatureBits(rawValue: 0x0002)
}

public struct ProtocolInfo
{
    public let specVersion : PKOCProtocolVersion?
    public let rawSpecByte : UInt8
    public let vendorSubVersion : UInt16
    public let featureBits : PKOCFeatureBits

    public init(specVersion: PKOCProtocolVersion?, rawSpecByte: UInt8, vendorSubVersion: UInt16, featureBits: PKOCFeatureBits)
    {
        self.specVersion = specVersion
        self.rawSpecByte = rawSpecByte
        self.vendorSubVersion = vendorSubVersion
        self.featureBits = featureBits
    }
}
