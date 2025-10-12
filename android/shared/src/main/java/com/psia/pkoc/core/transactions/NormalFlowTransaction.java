package com.psia.pkoc.core.transactions;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.Nullable;

import com.psia.pkoc.core.CryptoProvider;
import com.psia.pkoc.core.ReaderUnlockStatus;
import com.psia.pkoc.core.ValidationResult;
import com.psia.pkoc.core.interfaces.Transaction;
import com.psia.pkoc.core.interfaces.TransactionMessage;
import com.psia.pkoc.core.messages.DeviceCredentialMessage;
import com.psia.pkoc.core.messages.ReaderIdentifierMessage;
import com.psia.pkoc.core.messages.ReaderResponseMessage;
import com.psia.pkoc.core.packets.LastUpdateTimePacket;
import com.psia.pkoc.core.packets.ReaderIdentifierPacket;
import com.psia.pkoc.core.packets.ReaderNoncePacket;
import com.psia.pkoc.core.validations.SuccessResult;
import com.psia.pkoc.core.validations.UnexpectedPacketResult;

import java.security.SecureRandom;

public class NormalFlowTransaction<TPacket, TType> implements Transaction
{
    private static final String TAG = "NormalFlowTransaction";

    private final boolean isDevice;
    protected byte[] toWrite;
    @Nullable
    private final Activity activity;

    public TransactionMessage<TPacket> currentMessage;

    public NormalFlowTransaction(boolean _isDevice)
    {
        this(_isDevice, null);
    }

    public NormalFlowTransaction(boolean _isDevice, @Nullable Activity _activity)
    {
        Log.d(TAG, "Constructor called with isDevice: " + _isDevice);
        currentMessage = new ReaderIdentifierMessage<>();
        isDevice = _isDevice;
        activity = _activity;

        if (!isDevice)
        {
            Log.d(TAG, "Device-initiated transaction, preparing ReaderIdentifierMessage.");
            byte[] transactionId = new byte[16]; // Generate or obtain a transaction ID
            new SecureRandom().nextBytes(transactionId); // Ensure it's random
            var transactionIdPacket = new ReaderNoncePacket(transactionId);

            byte[] readerIdentifier = new byte[32]; // Obtain the reader identifier
            new SecureRandom().nextBytes(readerIdentifier); // Ensure it's random
            var readerIdentifierPacket = new ReaderIdentifierPacket(readerIdentifier);

            currentMessage = new ReaderIdentifierMessage<>(transactionIdPacket, readerIdentifierPacket);
            toWrite = currentMessage.encodePackets();
        }
    }

    public ValidationResult processNewPacket(TPacket packet)
    {
        Log.d(TAG, "processNewPacket called. Current message type: " + currentMessage.getClass().getSimpleName());
        if (currentMessage instanceof ReaderIdentifierMessage)
        {
            var readerIdentifierMessage = (ReaderIdentifierMessage<TPacket>)currentMessage;
            if (isDevice)
            {
                Log.d(TAG, "Processing as ReaderIdentifierMessage.");
                ValidationResult vr = readerIdentifierMessage.processNewPacket(packet);
                var messageValidation = readerIdentifierMessage.validate();
                if (vr.isValid && messageValidation.isValid)
                {
                    Log.i(TAG, "ReaderIdentifierMessage processed successfully. Transitioning to DeviceCredentialMessage.");

                    if (readerIdentifierMessage.getReaderNonce() != null)
                    {
                        currentMessage = DeviceCredentialMessage.forNfc(
                            readerIdentifierMessage.getReaderNonce(),
                            readerIdentifierMessage.getProtocolVersion()
                        );
                    }
                    else
                    {
                        if (activity != null)
                        {
                            currentMessage = DeviceCredentialMessage.forBleNormal(
                                readerIdentifierMessage.getCompressedKey(),
                                readerIdentifierMessage.getProtocolVersion(),
                                new LastUpdateTimePacket(CryptoProvider.getLastUpdateTime(activity))
                            );
                        }
                        else
                        {
                            // This case should not happen in a normal flow, but we handle it for robustness.
                            currentMessage = DeviceCredentialMessage.forBleNormal(
                                readerIdentifierMessage.getCompressedKey(),
                                readerIdentifierMessage.getProtocolVersion(),
                                null
                            );
                        }
                    }

                    toWrite = currentMessage.encodePackets();
                    return new SuccessResult();
                }
                else if (messageValidation.cancelTransaction)
                {
                    Log.w(TAG, "Transaction cancelled during ReaderIdentifierMessage processing.");
                    return messageValidation;
                }
                Log.d(TAG, "ReaderIdentifierMessage processing returned validation result: " + vr.isValid);
                return vr;
            }
        }

        if (currentMessage instanceof DeviceCredentialMessage)
        {
            Log.d(TAG, "Transitioning from DeviceCredentialMessage to ReaderResponseMessage.");
            currentMessage = new ReaderResponseMessage<>();
        }

        if (currentMessage instanceof ReaderResponseMessage)
        {
            Log.d(TAG, "Processing as ReaderResponseMessage.");
            var readerResponseMessage = (ReaderResponseMessage<TPacket, TType>) currentMessage;
            var vr = readerResponseMessage.processNewPacket(packet);
            var messageValidation = readerResponseMessage.validate();
            if (vr.isValid && messageValidation.isValid)
            {
                Log.i(TAG, "ReaderResponseMessage processed successfully.");
                return new SuccessResult();
            }
            else if (messageValidation.cancelTransaction)
            {
                Log.w(TAG, "Transaction cancelled during ReaderResponseMessage processing.");
                return messageValidation;
            }
            Log.d(TAG, "ReaderResponseMessage processing returned validation result: " + vr.isValid);
            return vr;
        }

        Log.e(TAG, "Unexpected packet received for the current state.");
        return new UnexpectedPacketResult();
    }

    @Override
    public ReaderUnlockStatus getReaderUnlockStatus()
    {
        if (currentMessage instanceof ReaderResponseMessage)
        {
            var responsePacket = ((ReaderResponseMessage<TPacket, TType>)currentMessage).getResponsePacket();
            if (responsePacket != null)
            {
                return responsePacket.getReaderUnlockStatus();
            }
        }

        return ReaderUnlockStatus.Unknown;
    }

    @Override
    public ValidationResult processNewData(byte[] data)
    {
        return new SuccessResult();
    }

    @Override
    public byte[] toWrite()
    {
        if (toWrite == null)
        {
            return null;
        }
        byte[] toReturn = toWrite.clone();
        toWrite = null;
        return toReturn;
    }
}
