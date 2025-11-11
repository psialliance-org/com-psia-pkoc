package com.psia.pkoc.core.messages;

import android.util.Log;

import com.psia.pkoc.core.BLE_Packet;
import com.psia.pkoc.core.BLE_PacketType;
import com.psia.pkoc.core.ReaderUnlockStatus;
import com.psia.pkoc.core.TLVProvider;
import com.psia.pkoc.core.ValidationResult;
import com.psia.pkoc.core.interfaces.TransactionMessage;
import com.psia.pkoc.core.packets.ResponsePacket;
import com.psia.pkoc.core.validations.SuccessResult;
import com.psia.pkoc.core.validations.UnexpectedPacketResult;
import com.psia.pkoc.core.validations.ValidatedBeforeCompleteResult;

public class ReaderResponseMessage<TPacket> implements TransactionMessage<TPacket>
{
    private static final String TAG = "ReaderResponseMessage";
    private ResponsePacket responsePacket;

    public ReaderResponseMessage()
    {
    }

    public ReaderResponseMessage(ReaderUnlockStatus status)
    {
        Log.d(TAG, "Constructor called with status: " + status);
        responsePacket = new ResponsePacket(status);
    }

    public ResponsePacket getResponsePacket()
    {
        return responsePacket;
    }

    public ValidationResult processNewPacket(TPacket packet)
    {
        Log.d(TAG, "processNewPacket called.");
        if (packet instanceof BLE_Packet && ((BLE_Packet) packet).PacketType == BLE_PacketType.Response)
        {
            Log.d(TAG, "Processing BLE Response packet.");
            ResponsePacket obj = new ResponsePacket(((BLE_Packet) packet).Data);
            var validationResult = obj.validate();
            if (validationResult instanceof SuccessResult)
            {
                responsePacket = obj;
                Log.i(TAG, "BLE Response packet processed successfully.");
                return new SuccessResult();
            }
            Log.w(TAG, "BLE Response packet validation failed.");
            return validationResult;
        }

        Log.w(TAG, "Unexpected packet type received: " + packet.getClass().getSimpleName());
        return new UnexpectedPacketResult();
    }

    public ValidationResult validate()
    {
        Log.d(TAG, "validate called.");
        if (responsePacket != null)
        {
            Log.i(TAG, "Validation successful.");
            return new SuccessResult();
        }

        Log.w(TAG, "Validation failed: responsePacket is null.");
        return new ValidatedBeforeCompleteResult();
    }

    public byte[] encodePackets()
    {
        Log.d(TAG, "encodePackets called.");
        var response = responsePacket.encode();
        return TLVProvider.GetBleTLV(BLE_PacketType.Response, response);
    }
}
