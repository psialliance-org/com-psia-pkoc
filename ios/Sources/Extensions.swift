import Foundation
import SwiftUI

extension String
{
    func hexStringToBytes() -> [UInt8]?
    {
        let string = self as String
        let length = string.count
        if length & 1 != 0
        {
            return nil
        }
        
        var bytes = [UInt8]()
        bytes.reserveCapacity(length/2)
        var index = string.startIndex
        for _ in 0..<length/2
        {
            let nextIndex = string.index(index, offsetBy: 2)
            if let b = UInt8(string[index..<nextIndex], radix: 16)
            {
                bytes.append(b)
            }
            else
            {
                return nil
            }
            index = nextIndex
        }
        return bytes
    }
}

extension Color
{
    init(hex: UInt, alpha: Double = 1)
    {
        self.init(
            .sRGB,
            red: Double((hex >> 16) & 0xff) / 255,
            green: Double((hex >> 08) & 0xff) / 255,
            blue: Double((hex >> 00) & 0xff) / 255,
            opacity: alpha
        )
    }
}

extension Data
{
    func hexadecimal() -> String
    {
        return map { String(format: "%02x", $0) }.joined(separator: "")
    }

    init?(hex: String)
    {
        guard hex.count.isMultiple(of: 2) else
        {
            return nil
        }
        
        let chars = hex.map { $0 }
        let bytes = stride(from: 0, to: chars.count, by: 2)
            .map{ String(chars[$0]) + String(chars[$0 + 1]) }
            .compactMap { UInt8($0, radix: 16) }
        
        guard hex.count / bytes.count == 2 else { return nil }
        self.init(bytes)
    }
}

extension UUID
{
    public func asUInt8Array() -> [UInt8]
    {
        let (u1, u2, u3, u4, u5, u6, u7, u8, u9, u10, u11, u12, u13, u14, u15, u16) = self.uuid
        return [u1, u2, u3, u4, u5, u6, u7, u8, u9, u10, u11, u12, u13, u14, u15, u16]
    }
    
    public func asData() -> Data
    {
        return Data(self.asUInt8Array())
    }
}
