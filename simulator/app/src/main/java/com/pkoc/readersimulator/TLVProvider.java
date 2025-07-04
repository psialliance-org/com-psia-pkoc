package com.pkoc.readersimulator;

import static java.lang.System.arraycopy;

import android.util.Log;

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
    public static byte[] GetTLV(BLE_PacketType type, byte[] data)
    {
        byte[] prepend = new byte[] { type.getType(), (byte) data.length };
        return Arrays.concatenate(prepend, data);
    }

    /**
     * Get Value of encoded Type Length Value message
     * @param encodedData TLV encoded byte array
     * @return BLE_Packet containing type and decoded byte array
     */
    public static BLE_Packet GetValue(byte[] encodedData) {
        // Extract the Protocol Version (first byte)
        byte protocolVersion = encodedData[0];
        BLE_PacketType packetType = BLE_PacketType.decode(encodedData[0]);
        int length = encodedData[1];

        // Debugging: Log the Protocol Version, length, and the data being processed
        Log.d("TLVProvider", "Processing TLV: ProtocolVersion=" + protocolVersion + ", Type=" + packetType + ", Length=" + length);

        if (packetType == BLE_PacketType.Void) {
            Log.d("TLVProvider", "Skipping Void packet");
            return null;
        }

        if (length < 0 || length > encodedData.length - 2) {
            Log.e("TLVProvider", "Invalid length: " + length + " for data: " + java.util.Arrays.toString(encodedData));
            throw new NegativeArraySizeException("Invalid length: " + length);
        }

        byte[] decodedData = new byte[length];
        System.arraycopy(encodedData, 2, decodedData, 0, length);

        // Debugging: Log the decoded data
        Log.d("TLVProvider", "Decoded data: " + java.util.Arrays.toString(decodedData));

        return new BLE_Packet(packetType, decodedData);
    }

    /**
     * Get values of a message containing TLV encoded data
     * @param data byte array containing one or more TLV encoded messages
     * @return Array list of messages to be read
     */
    public static ArrayList<BLE_Packet> GetValues(byte[] data) {
        ArrayList<BLE_Packet> gattTypeDataArrayList = new ArrayList<>();

        if (data.length < 3) // not long enough to be a TLV
        {
            return gattTypeDataArrayList;
        }

        int processedDataLength = 0;
        do {
            byte[] dataToProcess = new byte[data.length - processedDataLength];
            System.arraycopy(data, processedDataLength, dataToProcess, 0, data.length - processedDataLength);

            // Debugging: Log the data being processed
            Log.d("TLVProvider", "Data to process: " + java.util.Arrays.toString(dataToProcess));

            BLE_Packet gTypeData = GetValue(dataToProcess);
            if (gTypeData != null) {
                gattTypeDataArrayList.add(gTypeData);

                // Debugging: Log the processed data length
                Log.d("TLVProvider", "Processed data length: " + processedDataLength + ", Packet length: " + gTypeData.Data.length);

                processedDataLength += gTypeData.Data.length + 2;
            } else {
                // Skip the void packet
                processedDataLength += 3; // Skip the protocol version, type, and length bytes
            }
        } while (processedDataLength < data.length);

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
