package com.psia.pkoc.core.messages;

import android.util.Log;

import com.psia.pkoc.core.BLE_Packet;
import com.psia.pkoc.core.BLE_PacketType;
import com.psia.pkoc.core.CryptoProvider;
import com.psia.pkoc.core.TLVProvider;
import com.psia.pkoc.core.ValidationResult;
import com.psia.pkoc.core.interfaces.TransactionMessage;
import com.psia.pkoc.core.packets.DigitalSignaturePacket;
import com.psia.pkoc.core.validations.SuccessResult;
import com.psia.pkoc.core.validations.UnexpectedPacketResult;
import com.psia.pkoc.core.validations.ValidatedBeforeCompleteResult;

public class ReaderDigitalSignatureMessage implements TransactionMessage<BLE_Packet>
{
    private static final String TAG = "ReaderDigitalSignatureMessage";
    private DigitalSignaturePacket readerSignaturePacket;
    private final byte[] sitePublicKey;
    private final byte[] originalMessage;

    public ReaderDigitalSignatureMessage()
    {
        this.sitePublicKey = null;
        this.originalMessage = null;
    }

    public ReaderDigitalSignatureMessage(byte[] sitePublicKey, byte[] originalMessage)
    {
        this.sitePublicKey = sitePublicKey;
        this.originalMessage = originalMessage;
    }


    @Override
    public byte[] encodePackets()
    {
        Log.d(TAG, "encodePackets called.");
        var signature = readerSignaturePacket.encode();
        return TLVProvider.GetBleTLV(BLE_PacketType.DigitalSignature, signature);
    }

    public ValidationResult handleReaderSignature(byte[] data)
    {
        Log.d(TAG, "handleReaderSignature called.");
        var obj = new DigitalSignaturePacket(data);
        ValidationResult vr = obj.validate();
        if (vr instanceof SuccessResult)
        {
            readerSignaturePacket = obj;
            Log.i(TAG, "Reader signature packet processed successfully.");
        }
        else
        {
            Log.w(TAG, "Reader signature packet validation failed.");
        }
        return vr;
    }

    @Override
    public ValidationResult processNewPacket(BLE_Packet blePacket)
    {
        Log.d(TAG, "processNewPacket called.");
        if (blePacket.PacketType == BLE_PacketType.DigitalSignature)
        {
            return handleReaderSignature(blePacket.Data);
        }

        Log.w(TAG, "Unexpected packet type received: " + blePacket.PacketType);
        return new UnexpectedPacketResult();
    }

    @Override
    public ValidationResult validate()
    {
        Log.d(TAG, "validate called.");
        if (readerSignaturePacket == null)
        {
            Log.w(TAG, "Validation failed: readerSignaturePacket is null.");
            return new ValidatedBeforeCompleteResult();
        }

        if (sitePublicKey == null || originalMessage == null)
        {
            // This is not an ECDHE flow that requires signature validation at this level.
            Log.d(TAG, "Validation skipped, not an ECDHE flow.");
            return new SuccessResult();
        }

        boolean readerValid = CryptoProvider.validateSignedMessage(
                sitePublicKey,
                originalMessage,
                readerSignaturePacket.encode());

        if (!readerValid)
        {
            Log.e(TAG, "Reader signature validation failed.");
            return new UnexpectedPacketResult();
        }

        Log.i(TAG, "Reader signature is valid.");
        return new SuccessResult();
    }
}
