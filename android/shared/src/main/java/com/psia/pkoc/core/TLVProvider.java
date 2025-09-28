package com.psia.pkoc.core;

import static java.lang.System.arraycopy;

import android.util.Log;

import org.bouncycastle.util.Arrays;

import java.util.ArrayList;

/**
 * Type Length Value Provider
 */
public class TLVProvider
{
    public static byte[] GetBleTLV(BLE_PacketType type, byte[] value)
    {
        return getTLV(type, value, BLE_CODEC);
    }

    public static BLE_Packet GetBleValue(byte[] encoded)
    {
        return getValue(encoded, BLE_CODEC);
    }

    public static ArrayList<BLE_Packet> GetBleValues(byte[] buffer)
    {
        return getValues(buffer, BLE_CODEC);
    }

    public static byte[] GetNfcTLV(NFC_PacketType type, byte[] value)
    {
        return getTLV(type, value, NFC_CODEC);
    }

    public static NFC_Packet GetNfcValue(byte[] encoded)
    {
        return getValue(encoded, NFC_CODEC);
    }

    public static ArrayList<NFC_Packet> GetNfcValues(byte[] buffer)
    {
        return getValues(buffer, NFC_CODEC);
    }

    private static final TLVEncoder<BLE_PacketType, BLE_Packet> BLE_CODEC = new TLVEncoder<BLE_PacketType, BLE_Packet>()
    {
        @Override
        public byte toByte(BLE_PacketType type)
        {
            return type.getType();
        }

        @Override
        public BLE_PacketType decode(byte typeByte)
        {
            return BLE_PacketType.decode(typeByte);
        }

        @Override
        public BLE_Packet newPacket(BLE_PacketType type, byte[] value)
        {
            return new BLE_Packet(type, value);
        }
    };

    private static final TLVEncoder<NFC_PacketType, NFC_Packet> NFC_CODEC = new TLVEncoder<NFC_PacketType, NFC_Packet>()
    {
        @Override
        public byte toByte(NFC_PacketType type)
        {
            return type.getType();
        }

        @Override
        public NFC_PacketType decode(byte typeByte)
        {
            return NFC_PacketType.decode(typeByte);
        }

        @Override
        public NFC_Packet newPacket(NFC_PacketType type, byte[] value)
        {
            return new NFC_Packet(type, value);
        }
    };

    /**
     * Get Type Length Value encoded byte array
     * @param type Message type
     * @param data Message content
     * @return TLV encoded byte array
     */
    private static <TType, TPacket> byte[] getTLV(TType type, byte[] data, TLVEncoder<TType, TPacket> codec)
    {
        byte[] prepend = new byte[] { codec.toByte(type), (byte) data.length };
        return Arrays.concatenate(prepend, data);
    }

    /**
     * Get Value of encoded Type Length Value message
     * @param encodedData TLV encoded byte array
     * @return BLE_Packet containing type and decoded byte array
     */
    private static <TType, TPacket> TPacket getValue(byte[] encodedData, TLVEncoder<TType, TPacket> codec)
    {
        if (encodedData.length < 3) // not long enough to be a TLV
        {
            return null;
        }

        TType packetType = codec.decode(encodedData[0]);
        int length = encodedData[1] & 0xFF;

        Log.d("TLVProvider", "Processing TLV: Type=" + packetType + ", Length=" + length);

        if (packetType == BLE_PacketType.Void)
        {
            Log.d("TLVProvider", "Skipping Void packet");
            return null;
        }

        if (length > encodedData.length - 2)
        {
            Log.e("TLVProvider", "Invalid length: " + length + " for data: " + java.util.Arrays.toString(encodedData));
            return null;
        }

        byte[] decodedData = new byte[length];
        System.arraycopy(encodedData, 2, decodedData, 0, length);

        Log.d("TLVProvider", "Decoded data: " + java.util.Arrays.toString(decodedData));


        return codec.newPacket(packetType, decodedData);
    }

    /**
     * Get values of a message containing TLV encoded data
     * @param buffer byte array containing one or more TLV encoded messages
     * @return Array list of messages to be read
     */
    private static <TType, TPacket> ArrayList<TPacket> getValues(byte[] buffer, TLVEncoder<TType, TPacket> codec)
    {
        ArrayList<TPacket> packets = new ArrayList<>();
        if (buffer == null || buffer.length == 0)
        {
            return packets;
        }

        int cursor = 0;
        while (cursor + 2 <= buffer.length) // at least Type + Length remain
        {
            int start = cursor;

            byte typeByte = buffer[cursor++];
            int length = buffer[cursor++] & 0xFF;

            // If not enough bytes remain for the value, stop parsing (graceful fail for trailing noise)
            if (cursor + length > buffer.length)
            {
                Log.w("TLVProvider", "Remaining buffer smaller than TLV length. aborting. start=" + start + " length=" + length + " remaining=" + (buffer.length - cursor));
                break;
            }

            // Slice out the value bytes
            byte[] value = new byte[length];
            if (length > 0)
            {
                arraycopy(buffer, cursor, value, 0, length);
            }
            cursor += length;

            // Build packet and append
            TType type = codec.decode(typeByte);
            packets.add(codec.newPacket(type, value));
        }

        return packets;
    }
}