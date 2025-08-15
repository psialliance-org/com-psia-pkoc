package com.psia.pkoc;

import static java.lang.System.arraycopy;
import android.util.Log;
import org.bouncycastle.util.Arrays;
import java.util.ArrayList;

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
        if (encodedData.length < 3) // not long enough to be a TLV
        {
            return null;
        }

        BLE_PacketType packetType = BLE_PacketType.decode(encodedData[0]);
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

        return new BLE_Packet(packetType, decodedData);
    }

    /**
     * Get Value of encoded Type Length Value message
     * @param encodedData TLV encoded byte array
     * @return NFC_Packet containing type and decoded byte array
     */
    public static NFC_Packet GetNfcValue(byte[] encodedData)
    {
        if (encodedData.length < 3) // not long enough to be a TLV
        {
            return null;
        }

        NFC_PacketType packetType = NFC_PacketType.decode(encodedData[0]);

        int length = encodedData[1] & 0xFF;

        Log.d("TLVProvider", "Processing TLV: Type=" + packetType + ", Length=" + length);

        if (packetType == NFC_PacketType.Void)
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

        if (data.length < 3) // not long enough to be a TLV
        {
            return gattTypeDataArrayList;
        }

        int processedDataLength = 0;
        do
        {
            byte[] dataToProcess = new byte[data.length - processedDataLength];
            System.arraycopy(data, processedDataLength, dataToProcess, 0, data.length - processedDataLength);

            // Debugging: Log the data being processed
            Log.d("TLVProvider", "Data to process: " + java.util.Arrays.toString(dataToProcess));

            BLE_Packet gTypeData = GetBleValue(dataToProcess);
            if (gTypeData != null)
            {
                gattTypeDataArrayList.add(gTypeData);

                // Debugging: Log the processed data length
                Log.d("TLVProvider", "Processed data length: " + processedDataLength + ", Packet length: " + gTypeData.Data.length);

                processedDataLength += gTypeData.Data.length + 2;
            }
            else
            {
                // we have either found an unrecognized TLV or have happened upon an incorrectly
                // formatted TLV. Regardless, there is not more usable data left to process
                return gattTypeDataArrayList;
            }
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
            if (gTypeData != null)
            {
                gattTypeDataArrayList.add(gTypeData);
                processedDataLength += gTypeData.Data.length + 2;
            }
            else
            {
                // we have either found an unrecognized TLV or have happened upon an incorrectly
                // formatted TLV. Regardless, there is not more usable data left to process
                return gattTypeDataArrayList;
            }
        }
        while (processedDataLength < data.length);

        return gattTypeDataArrayList;
    }
}
