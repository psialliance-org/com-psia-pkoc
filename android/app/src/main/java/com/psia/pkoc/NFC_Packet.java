package com.psia.pkoc;

/**
 * BLE packet
 */
public class NFC_Packet
{
    /**
     * Packet type
     */
    public final NFC_PacketType PacketType;

    /**
     * data
     */
    public final byte[] Data;

    /**
     * Constructor
     * @param packetType Packet type
     * @param data data
     */
    public NFC_Packet(NFC_PacketType packetType, byte[] data)
    {
        PacketType = packetType;
        Data = data;
    }
}
