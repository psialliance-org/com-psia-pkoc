package com.pkoc.readersimulator;

import static android.content.ContentValues.TAG;

import android.util.Log;

import java.util.Arrays;

/**
 * BLE packet type for TLV-encoded data
 */
public enum BLE_PacketType
{
    Void ((byte) 0x00),
    PublicKey ((byte) 0x01),
    CompressedTransientPublicKey ((byte) 0x02),
    DigitalSignature ((byte) 0x03),
    Response ((byte) 0x04),
    UncompressedTransientPublicKey ((byte) 0x07),
    LastUpdateTime ((byte) 0x09),
    ProtocolVersion ((byte) 0x0C),
    ReaderLocationIdentifier ((byte) 0x0D),
    SiteIdentifier ((byte) 0x0E),
    EncryptedDataFollows ((byte) 0x40),
    ManufacturerSpecificData ((byte) 0x80);

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
    public static BLE_PacketType decode (byte data)
    {
        for (int a = 0; a < BLE_PacketType.values().length; a++)
        {
            if (BLE_PacketType.values()[a].getType() == data)
            {
                Log.d(TAG, "PaketType: " + Arrays.toString(BLE_PacketType.values()));
                return BLE_PacketType.values()[a];
            }
        }

        return Void;
    }

    /**
     * Parameterized constructor
     * @param typeValue Type value as a byte
     */
    BLE_PacketType(byte typeValue)
    {
        type = typeValue;
    }
}
