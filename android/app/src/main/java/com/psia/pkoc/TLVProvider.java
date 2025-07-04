package com.psia.pkoc;

import static java.lang.System.arraycopy;

import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.BigIntegers;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Type Length Value Provider
 */
public class TLVProvider
{
    /**
     * Get Type Length Value encoded byte array
     * @param type Message type
     * @param data Message content
     * @return TLV encoded byte array
     */
    public static byte[] GetBleTLV(BLE_PacketType type, byte[] data)
    {
        byte[] prepend = new byte[] { type.getType(), (byte) data.length };
        return Arrays.concatenate(prepend, data);
    }

    /**
     * Get Type Length Value encoded byte array
     * @param type Message type
     * @param data Message content
     * @return TLV encoded byte array
     */
    public static byte[] GetNfcTLV(NFC_PacketType type, byte[] data)
    {
        byte[] prepend = new byte[] { type.getType(), (byte) data.length };
        return Arrays.concatenate(prepend, data);
    }

    /**
     * Get Value of encoded Type Length Value message
     * @param encodedData TLV encoded byte array
     * @return BLE_Packet containing type and decoded byte array
     */
    public static BLE_Packet GetBleValue(byte[] encodedData)
    {
        BLE_PacketType packetType = BLE_PacketType.decode(encodedData[0]);

        byte[] decodedData = new byte[encodedData[1]];
        arraycopy(encodedData, 2, decodedData, 0, encodedData[1]);

        return new BLE_Packet(packetType, decodedData);
    }

    /**
     * Get Value of encoded Type Length Value message
     * @param encodedData TLV encoded byte array
     * @return NFC_Packet containing type and decoded byte array
     */
    public static NFC_Packet GetNfcValue(byte[] encodedData)
    {
        NFC_PacketType packetType = NFC_PacketType.decode(encodedData[0]);

        byte[] decodedData = new byte[encodedData[1]];
        arraycopy(encodedData, 2, decodedData, 0, encodedData[1]);

        return new NFC_Packet(packetType, decodedData);
    }


    /**
     * Get values of a message containing TLV encoded data
     * @param data byte array containing one or more TLV encoded messages
     * @return Array list of messages to be read
     */
    public static ArrayList<BLE_Packet> GetBleValues(byte[] data)
    {
        ArrayList<BLE_Packet> gattTypeDataArrayList = new ArrayList<>();

        if (data.length < 2) //not long enough to be a TLV
        {
            return gattTypeDataArrayList;
        }

        int processedDataLength = 0;
        do
        {
            byte[] dataToProcess = new byte[data.length - processedDataLength];
            arraycopy(data, processedDataLength, dataToProcess, 0, data.length - processedDataLength);
            BLE_Packet gTypeData = GetBleValue(dataToProcess);
            gattTypeDataArrayList.add(gTypeData);
            processedDataLength += gTypeData.Data.length + 2;
        }
        while (processedDataLength < data.length);

        return gattTypeDataArrayList;
    }

    /**
     * Get values of a message containing TLV encoded data
     * @param data byte array containing one or more TLV encoded messages
     * @return Array list of messages to be read
     */
    public static ArrayList<NFC_Packet> GetNfcValues(byte[] data)
    {
        ArrayList<NFC_Packet> gattTypeDataArrayList = new ArrayList<>();

        if (data.length < 2) //not long enough to be a TLV
        {
            return gattTypeDataArrayList;
        }

        int processedDataLength = 0;
        do
        {
            byte[] dataToProcess = new byte[data.length - processedDataLength];
            arraycopy(data, processedDataLength, dataToProcess, 0, data.length - processedDataLength);
            NFC_Packet gTypeData = GetNfcValue(dataToProcess);
            gattTypeDataArrayList.add(gTypeData);
            processedDataLength += gTypeData.Data.length + 2;
        }
        while (processedDataLength < data.length);

        return gattTypeDataArrayList;
    }

    /**
     * Remove ASN header from signature
     * @param signature ASN1/DER encoded signature
     * @return 64 byte byte arraying containing r|s
     */
    public static byte[] RemoveASNHeaderFromSignature(byte[] signature)
    {
        ASN1Sequence seq = ASN1Sequence.getInstance(signature);
        byte[] r = BigIntegers.asUnsignedByteArray(ASN1Integer.getInstance(seq.getObjectAt(0)).getPositiveValue());
        byte[] s = BigIntegers.asUnsignedByteArray(ASN1Integer.getInstance(seq.getObjectAt(1)).getPositiveValue());

        byte[] r32 = new byte[32], s32 = new byte[32];
        arraycopy(r, 0, r32, 32 - r.length, r.length);
        arraycopy(s, 0, s32, 32 - s.length, s.length);

        return Arrays.concatenate(r32, s32);
    }

    /**
     * Get byte array from GUID
     * @param uuid GUID
     * @return Byte array containing GUID
     */
    public static byte[] getByteArrayFromGuid(UUID uuid)
    {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }
}
