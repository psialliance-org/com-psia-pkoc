package com.psia.pkoc;

/**
 * BLE packet type for TLV-encoded data
 */
public enum NFC_PacketType
{
    Void ((byte) 0x00),
    ProtocolVersion ((byte) 0x5C),
    TransactionIdentifier ((byte) 0x4C),
    ReaderIdentifier ((byte) 0x4D),
    DigitalSignature ((byte) 0x9E),
    UncompressedPublicKey ((byte) 0x5A);

    private final byte type;

    /**
     * Get type
     * @return packet type as a byte
     */
    public byte getType() { return type; }

    /**
     * Decode
     * @param data Single byte signalling type of packet
     * @return Packet type as an enum
     */
    public static NFC_PacketType decode (byte data)
    {
        for (int a = 0; a < NFC_PacketType.values().length; a++)
        {
            if (NFC_PacketType.values()[a].getType() == data)
            {
                return NFC_PacketType.values()[a];
            }
        }

        return Void;
    }

    /**
     * Parameterized constructor
     * @param typeValue Type value as a byte
     */
    NFC_PacketType (byte typeValue)
    {
        type = typeValue;
    }
}
