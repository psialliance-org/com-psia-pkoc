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

public class NormalFlowTransaction<TPacket> implements Transaction
{
    private static final String TAG = "NormalFlowTransaction";

    protected final boolean isDevice;
    protected byte[] toWrite;
    @Nullable
    private final Activity activity;
    private DeviceCredentialMessage<TPacket> deviceCredentialMessage;

    public TransactionMessage<TPacket> currentMessage;
    private ReaderIdentifierMessage<TPacket> initialReaderMessage;

    public NormalFlowTransaction(boolean _isDevice)
    {
        this(_isDevice, null);
    }

    public NormalFlowTransaction(boolean _isDevice, @Nullable Activity _activity)
    {
        Log.d(TAG, "Constructor called with isDevice: " + _isDevice);
        isDevice = _isDevice;
        activity = _activity;

        if (!isDevice)
        {
            Log.d(TAG, "Reader-initiated transaction, preparing ReaderIdentifierMessage.");
            byte[] transactionId = new byte[16];
            new SecureRandom().nextBytes(transactionId);
            var transactionIdPacket = new ReaderNoncePacket(transactionId);

            byte[] readerIdentifier = new byte[32];
            new SecureRandom().nextBytes(readerIdentifier);
            var readerIdentifierPacket = new ReaderIdentifierPacket(readerIdentifier);

            initialReaderMessage = new ReaderIdentifierMessage<>(transactionIdPacket, readerIdentifierPacket);
            currentMessage = initialReaderMessage;
            toWrite = currentMessage.encodePackets();
        }
        else
        {
            currentMessage = new ReaderIdentifierMessage<>();
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
                    }

                    deviceCredentialMessage = (DeviceCredentialMessage<TPacket>) currentMessage;
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
            else
            {
                currentMessage = new DeviceCredentialMessage<>(initialReaderMessage.getReaderNonce().encode());
                deviceCredentialMessage = (DeviceCredentialMessage<TPacket>) currentMessage;
                return currentMessage.processNewPacket(packet);
            }
        }

        if (currentMessage instanceof DeviceCredentialMessage)
        {
            Log.d(TAG, "Processing as DeviceCredentialMessage.");
            ValidationResult vr = currentMessage.processNewPacket(packet);
            var messageValidation = currentMessage.validate();
            if (vr.isValid && messageValidation.isValid)
            {
                Log.i(TAG, "DeviceCredentialMessage processed successfully. Transitioning to ReaderResponseMessage.");
                currentMessage = new ReaderResponseMessage<>();
                return new SuccessResult();
            }
            else if (messageValidation.cancelTransaction)
            {
                Log.w(TAG, "Transaction cancelled during DeviceCredentialMessage processing.");
                return messageValidation;
            }
            Log.d(TAG, "DeviceCredentialMessage processing returned validation result: " + vr.isValid);
            return vr;
        }

        if (currentMessage instanceof ReaderResponseMessage)
        {
            Log.d(TAG, "Processing as ReaderResponseMessage.");
            var readerResponseMessage = (ReaderResponseMessage<TPacket>) currentMessage;
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
            var responsePacket = ((ReaderResponseMessage<TPacket>)currentMessage).getResponsePacket();
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
        // To be implemented in subclass
        return new SuccessResult();
    }

    public byte[] getPublicKey()
    {
        if (deviceCredentialMessage != null && deviceCredentialMessage.getPublicKeyPacket() != null)
        {
            return deviceCredentialMessage.getPublicKeyPacket().encode();
        }
        return null;
    }

    public byte[] getSignature()
    {
        if (deviceCredentialMessage != null && deviceCredentialMessage.getSignaturePacket() != null)
        {
            return deviceCredentialMessage.getSignaturePacket().encode();
        }
        return null;
    }

    public byte[] getTransactionId()
    {
        if (initialReaderMessage != null && initialReaderMessage.getReaderNonce() != null)
        {
            return initialReaderMessage.getReaderNonce().encode();
        }
        return null;
    }

    public ValidationResult validate()
    {
        if (currentMessage instanceof DeviceCredentialMessage)
        {
            return currentMessage.validate();
        }
        if (currentMessage instanceof ReaderResponseMessage)
        {
            return currentMessage.validate();
        }
        return new ValidationResult(false, false, "Invalid message type for validation");
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
        if (isDevice && currentMessage instanceof DeviceCredentialMessage)
        {
            currentMessage = new ReaderResponseMessage<>();
        }
        return toReturn;
    }
}