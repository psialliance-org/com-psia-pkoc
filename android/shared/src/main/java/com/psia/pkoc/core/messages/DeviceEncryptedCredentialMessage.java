package com.psia.pkoc.core.messages;

import android.util.Log;

import com.psia.pkoc.core.BLE_Packet;
import com.psia.pkoc.core.BLE_PacketType;
import com.psia.pkoc.core.CryptoProvider;
import com.psia.pkoc.core.TLVProvider;
import com.psia.pkoc.core.ValidationResult;
import com.psia.pkoc.core.packets.EncryptedDataFollowsPacket;
import com.psia.pkoc.core.packets.LastUpdateTimePacket;
import com.psia.pkoc.core.packets.ProtocolVersionPacket;
import com.psia.pkoc.core.validations.SuccessResult;
import com.psia.pkoc.core.validations.UnexpectedPacketResult;
import com.psia.pkoc.core.validations.ValidatedBeforeCompleteResult;

public class DeviceEncryptedCredentialMessage extends DeviceCredentialMessage<BLE_Packet>
{
    private static final String TAG = "DeviceEncryptedCredentialMessage";
    private EncryptedDataFollowsPacket encryptedDataFollowsPacket;
    private final byte[] sharedSecret;
    private final int counter;

    public DeviceEncryptedCredentialMessage(byte[] toSign, ProtocolVersionPacket _protocolIdentifier, byte[] sharedSecret, int counter, LastUpdateTimePacket lastUpdateTime)
    {
        super(toSign, _protocolIdentifier, lastUpdateTime);
        this.sharedSecret = sharedSecret;
        this.counter = counter;
        Log.d(TAG, "Constructor called. toSign length: " + (toSign != null ? toSign.length : "null"));
    }

    @Override
    public byte[] encodePackets()
    {
        Log.d(TAG, "encodePackets called.");
        var toEncrypt = super.encodePackets();
        var encryptedPackets = CryptoProvider.getAES256(sharedSecret, toEncrypt, counter);
        return TLVProvider.GetBleTLV(BLE_PacketType.EncryptedDataFollows, encryptedPackets);
    }

    public ValidationResult handleEncryptedData(byte[] data)
    {
        Log.d(TAG, "handleEncryptedData called.");
        var obj = new EncryptedDataFollowsPacket(data);
        ValidationResult vr = obj.validate();
        if (vr instanceof SuccessResult)
        {
            encryptedDataFollowsPacket = obj;
            Log.i(TAG, "Encrypted data packet processed successfully.");
        }
        else
        {
            Log.w(TAG, "Encrypted data packet validation failed.");
        }
        return vr;
    }

    @Override
    public ValidationResult processNewPacket(BLE_Packet blePacket)
    {
        Log.d(TAG, "processNewPacket called.");
        if (blePacket.PacketType == BLE_PacketType.EncryptedDataFollows)
        {
            return handleEncryptedData(blePacket.Data);
        }

        Log.w(TAG, "Unexpected packet type received: " + blePacket.PacketType);
        return new UnexpectedPacketResult();
    }

    @Override
    public ValidationResult validate()
    {
        Log.d(TAG, "validate called.");
        if (encryptedDataFollowsPacket != null)
        {
            Log.d(TAG, "Encrypted data packet is not null, proceeding with decryption and validation.");
            var unencryptedData = CryptoProvider.getFromAES256(sharedSecret, encryptedDataFollowsPacket.encode(), counter);
            var packets = TLVProvider.GetBleValues(unencryptedData);
            for (var packet : packets)
            {
                var vr = super.processNewPacket(packet);
                if (!vr.isValid)
                {
                    Log.w(TAG, "Decrypted packet validation failed.");
                    return vr;
                }
            }

            Log.i(TAG, "All decrypted packets processed successfully.");
            return super.validate();
        }

        Log.w(TAG, "Validation failed: encryptedDataFollowsPacket is null.");
        return new ValidatedBeforeCompleteResult();
    }
}
