import Foundation

class TLVProvider
{
    static func getTLV(type : BLE_PacketType, data : [UInt8]) -> [UInt8]
    {
        var packet : [UInt8] = [ type.rawValue, UInt8(data.count) ]
        packet.append(contentsOf: data)
        return packet
    }
    
    static func getValue(data : [UInt8]) -> BLE_Packet
    {
        if data.isEmpty
        {
            return BLE_Packet(type:BLE_PacketType.ErrorPacket, data:[UInt8]())
        }
        
        if let packetType = BLE_PacketType(rawValue: data[0])
        {
            if (data.count < Int(data[1]) + 2)
            {
                return BLE_Packet(type:BLE_PacketType.ErrorPacket, data:[UInt8]())
            }
            
            let packetContent = Array(data[2..<Int(data[1])+2])
            return BLE_Packet(type: packetType, data: packetContent)
        }
        
        return BLE_Packet(type: BLE_PacketType.ErrorPacket, data: [UInt8]())

    }
    
    static func getValues(data : [UInt8]) -> [BLE_Packet]
    {
        var toReturn = [BLE_Packet]()
        
        if (data.count < 2) // not long enough to be a TLV
        {
            return toReturn
        }

        var processedDataLength = 0
        repeat
        {
            let packetData = getValue(data: Array(data[processedDataLength...data.count - 1]))
            toReturn.append(packetData)
            processedDataLength += packetData.data.count + 2
        }
        while (processedDataLength < data.count)
        
        return toReturn
    }
}
