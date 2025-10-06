package com.psia.pkoc.core;

/**
 * BLE packet
 */
public class BLE_Packet
{
    /**
     * Packet type
     */
    public final BLE_PacketType PacketType;

    /**
     * data
     */
    public final byte[] Data;

    /**
     * Constructor
     * @param packetType Packet type
     * @param data data
     */
    public BLE_Packet (BLE_PacketType packetType, byte[] data)
    {
        PacketType = packetType;
        Data = data;
    }
}
