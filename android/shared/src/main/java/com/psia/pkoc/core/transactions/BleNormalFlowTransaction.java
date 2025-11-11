package com.psia.pkoc.core.transactions;

import android.app.Activity;
import android.util.Log;

import com.psia.pkoc.core.BLE_Packet;
import com.psia.pkoc.core.TLVProvider;
import com.psia.pkoc.core.ValidationResult;
import com.psia.pkoc.core.validations.SuccessResult;

public class BleNormalFlowTransaction extends NormalFlowTransaction<BLE_Packet>
{
    private static final String TAG = "BleNormalFlowTransaction";

    public BleNormalFlowTransaction(boolean _isDevice, Activity _activity)
    {
        super(_isDevice, _activity);
        Log.d(TAG, "Constructor called with isDevice: " + _isDevice);
    }

    @Override
    public ValidationResult processNewData(byte[] data)
    {
        Log.d(TAG, "processNewData called with data length: " + (data != null ? data.length : "null"));
        var packets = TLVProvider.GetBleValues(data);
        Log.d(TAG, "Parsed " + packets.size() + " packets from data.");
        for (var packet : packets)
        {
            Log.d(TAG, "Processing packet: " + packet.PacketType);
            var vr = processNewPacket(packet);
            if (!vr.isValid)
            {
                Log.w(TAG, "Packet processing failed for packet type: " + packet.PacketType);
                return vr;
            }
            Log.d(TAG, "Packet processed successfully.");
        }

        Log.i(TAG, "All packets processed successfully.");
        return new SuccessResult();
    }
}
