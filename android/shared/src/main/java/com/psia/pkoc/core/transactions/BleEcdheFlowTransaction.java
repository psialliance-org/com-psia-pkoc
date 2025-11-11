package com.psia.pkoc.core.transactions;

import static com.psia.pkoc.core.CryptoProvider.CreateTransientKeyPair;
import static com.psia.pkoc.core.CryptoProvider.getSharedSecret;

import android.app.Activity;
import android.util.Log;

import com.psia.pkoc.core.BLE_Packet;
import com.psia.pkoc.core.CryptoProvider;
import com.psia.pkoc.core.ReaderDto;
import com.psia.pkoc.core.SiteDto;
import com.psia.pkoc.core.TLVProvider;
import com.psia.pkoc.core.ValidationResult;
import com.psia.pkoc.core.messages.DeviceEncryptedCredentialMessage;
import com.psia.pkoc.core.messages.DeviceIdentifierMessage;
import com.psia.pkoc.core.messages.ReaderDigitalSignatureMessage;
import com.psia.pkoc.core.messages.ReaderIdentifierMessage;
import com.psia.pkoc.core.messages.ReaderResponseMessage;
import com.psia.pkoc.core.packets.DeviceEphemeralPublicKeyPacket;
import com.psia.pkoc.core.packets.LastUpdateTimePacket;
import com.psia.pkoc.core.packets.ProtocolVersionPacket;
import com.psia.pkoc.core.validations.SuccessResult;
import com.psia.pkoc.core.validations.UnexpectedPacketResult;
import com.psia.pkoc.core.validations.UnrecognizedReaderResult;

import org.bouncycastle.util.Arrays;

import java.security.KeyPair;
import java.util.ArrayList;

public class BleEcdheFlowTransaction extends BleNormalFlowTransaction
{
    private static final String TAG = "BleEcdheFlowTransaction";

    private byte[] sharedSecret;
    private byte[] sitePublicKey;
    private ReaderIdentifierMessage<BLE_Packet> readerIdentifierMessage;
    private final ArrayList<SiteDto> siteDtos;
    private final ArrayList<ReaderDto> readerDtos;
    private int counter = 1;
    private final Activity activity;
    private DeviceEphemeralPublicKeyPacket deviceEphemeralPublicKeyPacket;

    public BleEcdheFlowTransaction(boolean _isDevice, ArrayList<SiteDto> _siteDtos, ArrayList<ReaderDto> _readerDtos, Activity _activity)
    {
        super(_isDevice, _activity);
        siteDtos = _siteDtos;
        readerDtos = _readerDtos;
        activity = _activity;
        Log.d(TAG, "Constructor called. siteDtos count: " + siteDtos.size() + ", readerDtos count: " + readerDtos.size());
    }

    @Override
    public ValidationResult processNewData(byte[] packets)
    {
        Log.d(TAG, "processNewData called with data length: " + (packets != null ? packets.length : "null"));
        for (var packet : TLVProvider.GetBleValues(packets))
        {
            Log.d(TAG, "Processing packet: " + packet.PacketType + ". Current message type: " + currentMessage.getClass().getSimpleName());
            if (currentMessage instanceof ReaderIdentifierMessage)
            {
                readerIdentifierMessage = (ReaderIdentifierMessage<BLE_Packet>) currentMessage;
                ValidationResult vr = readerIdentifierMessage.processNewPacket(packet);

                if (!vr.isValid)
                {
                    return vr;
                }

                var messageValidation = readerIdentifierMessage.validate();
                if (messageValidation.isValid)
                {
                    Log.i(TAG, "ReaderIdentifierMessage validated successfully. Checking site and reader recognition.");
                    boolean siteIdFound = false;
                    for (SiteDto siteDto : siteDtos)
                    {
                        if(java.util.Arrays.equals(siteDto.siteUUID, readerIdentifierMessage.getSiteId().encode()))
                        {
                            siteIdFound = true;
                            sitePublicKey = siteDto.publicKey;
                            Log.i(TAG, "Site recognized.");
                            break;
                        }
                    }

                    if (!siteIdFound)
                    {
                        Log.w(TAG, "Site ID not recognized.");
                        return new UnrecognizedReaderResult();
                    }

                    boolean readerIdFound = false;
                    for (ReaderDto readerDto : readerDtos)
                    {
                        if(java.util.Arrays.equals(readerDto.readerIdentifier, readerIdentifierMessage.getReaderLocationId().encode()) &&
                           java.util.Arrays.equals(readerDto.siteIdentifier, readerIdentifierMessage.getSiteId().encode()))
                        {
                            readerIdFound = true;
                            Log.i(TAG, "Reader recognized.");
                            break;
                        }
                    }

                    if(!readerIdFound)
                    {
                        Log.w(TAG, "Reader ID not recognized.");
                        return new UnrecognizedReaderResult();
                    }

                    Log.d(TAG, "Generating transient key pair and shared secret.");
                    KeyPair transientKeyPair = CreateTransientKeyPair();
                    assert transientKeyPair != null;
                    byte[] rawSharedSecret = getSharedSecret(
                            transientKeyPair.getPrivate(),
                            readerIdentifierMessage.getCompressedKey().encode());

                    sharedSecret = CryptoProvider.deriveAesKeyFromSharedSecretSimple(rawSharedSecret);

                    var publicKeyDes = transientKeyPair.getPublic().getEncoded();
                    var publicKey = CryptoProvider.getUncompressedPublicKeyBytes(publicKeyDes);
                    deviceEphemeralPublicKeyPacket = new DeviceEphemeralPublicKeyPacket(publicKey);
                    var protocolVersionPacket = new ProtocolVersionPacket(readerIdentifierMessage.getProtocolVersion().encode());

                    var deviceIdentifierMessage = new DeviceIdentifierMessage(deviceEphemeralPublicKeyPacket, protocolVersionPacket);
                    toWrite = deviceIdentifierMessage.encodePackets();

                    byte[] deviceX = CryptoProvider.getPublicKeyComponentX(publicKeyDes);
                    byte[] readerPk = readerIdentifierMessage.getCompressedKey().encode();
                    byte[] readerX = new byte[32];
                    java.lang.System.arraycopy(readerPk, 1, readerX, 0, 32);

                    byte[] originalMessage = Arrays.concatenate(readerIdentifierMessage.getSiteId().encode(), readerIdentifierMessage.getReaderLocationId().encode(), deviceX, readerX);

                    Log.i(TAG, "Transitioning to ReaderDigitalSignatureMessage.");
                    currentMessage = new ReaderDigitalSignatureMessage(sitePublicKey, originalMessage);
                    return new SuccessResult();
                }
                else if (messageValidation.cancelTransaction)
                {
                    Log.w(TAG, "Transaction cancelled during ReaderIdentifierMessage validation.");
                    return messageValidation;
                }
            }
            else if (currentMessage instanceof ReaderDigitalSignatureMessage)
            {
                var readerDigitalSignatureMessage = (ReaderDigitalSignatureMessage) currentMessage;
                var vr = readerDigitalSignatureMessage.processNewPacket(packet);

                if (!vr.isValid)
                {
                    return vr;
                }

                var messageValidation = readerDigitalSignatureMessage.validate();
                if (messageValidation.isValid)
                {
                    Log.i(TAG, "ReaderDigitalSignatureMessage validated successfully.");

                    byte[] toSign = Arrays.concatenate(
                        readerIdentifierMessage.getSiteId().encode(),
                        readerIdentifierMessage.getReaderLocationId().encode(),
                        deviceEphemeralPublicKeyPacket.getX(),
                        readerIdentifierMessage.getCompressedKey().getX()
                    );

                    var deviceEncryptedCredentialMessage = new DeviceEncryptedCredentialMessage(
                            toSign,
                            readerIdentifierMessage.getProtocolVersion(),
                            sharedSecret,
                            counter,
                            new LastUpdateTimePacket(CryptoProvider.getLastUpdateTime(activity))
                    );
                    toWrite = deviceEncryptedCredentialMessage.encodePackets();
                    counter++;
                    Log.i(TAG, "Transitioning to ReaderResponseMessage.");
                    currentMessage = new ReaderResponseMessage<>();
                    return new SuccessResult();
                }
                else if (messageValidation.cancelTransaction)
                {
                    Log.w(TAG, "Transaction cancelled during ReaderDigitalSignatureMessage validation.");
                    return messageValidation;
                }
            }
            else if (currentMessage instanceof ReaderResponseMessage)
            {
                var readerResponseMessage = (ReaderResponseMessage<BLE_Packet>) currentMessage;
                var vr = readerResponseMessage.processNewPacket(packet);

                if (!vr.isValid)
                {
                    return vr;
                }

                var messageValidation = readerResponseMessage.validate();
                if (messageValidation.isValid)
                {
                    Log.i(TAG, "ReaderResponseMessage validated successfully. Transaction complete.");
                    return new SuccessResult();
                }
                else if (messageValidation.cancelTransaction)
                {
                    Log.w(TAG, "Transaction cancelled during ReaderResponseMessage validation.");
                    return messageValidation;
                }
            }
        }
        Log.e(TAG, "No more packets to process, but transaction is not complete.");
        return new UnexpectedPacketResult();
    }
}
