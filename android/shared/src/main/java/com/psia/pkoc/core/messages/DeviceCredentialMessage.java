package com.psia.pkoc.core.messages;

import android.util.Log;

import com.psia.pkoc.core.BLE_Packet;
import com.psia.pkoc.core.BLE_PacketType;
import com.psia.pkoc.core.CryptoProvider;
import com.psia.pkoc.core.NFC_Packet;
import com.psia.pkoc.core.NFC_PacketType;
import com.psia.pkoc.core.TLVProvider;
import com.psia.pkoc.core.ValidationResult;
import com.psia.pkoc.core.interfaces.TransactionMessage;
import com.psia.pkoc.core.packets.DeviceEphemeralPublicKeyPacket;
import com.psia.pkoc.core.packets.DigitalSignaturePacket;
import com.psia.pkoc.core.packets.LastUpdateTimePacket;
import com.psia.pkoc.core.packets.ProtocolVersionPacket;
import com.psia.pkoc.core.packets.ReaderEphemeralPublicKeyPacket;
import com.psia.pkoc.core.packets.ReaderLocationIdentifierPacket;
import com.psia.pkoc.core.packets.ReaderNoncePacket;
import com.psia.pkoc.core.packets.SiteIdentifierPacket;
import com.psia.pkoc.core.packets.UncompressedPublicKeyPacket;
import com.psia.pkoc.core.validations.InvalidSignatureResult;
import com.psia.pkoc.core.validations.SuccessResult;
import com.psia.pkoc.core.validations.UnexpectedPacketResult;
import com.psia.pkoc.core.validations.ValidatedBeforeCompleteResult;

import org.bouncycastle.util.Arrays;

public class DeviceCredentialMessage<TPacket> implements TransactionMessage<TPacket>
{
    private static final String TAG = "DeviceCredentialMessage";

    private final byte[] originalMessage;
    private UncompressedPublicKeyPacket uncompressedPublicKey;
    private DigitalSignaturePacket digitalSignature;
    private LastUpdateTimePacket lastUpdateTime;
    private final ProtocolVersionPacket protocolIdentifier;

    protected DeviceCredentialMessage(byte[] toSign, ProtocolVersionPacket _protocolIdentifier, LastUpdateTimePacket _lastUpdateTime)
    {
        Log.d(TAG, "Constructor called. toSign length: " + (toSign != null ? toSign.length : "null"));
        protocolIdentifier = _protocolIdentifier;
        lastUpdateTime = _lastUpdateTime;

        var publicKey = CryptoProvider.getUncompressedPublicKeyBytes();
        uncompressedPublicKey = new UncompressedPublicKeyPacket(publicKey);
        Log.d(TAG, "Public key initialized.");

        originalMessage = toSign;
        byte[] SignedMessage = CryptoProvider.GetSignedMessage(toSign);
        byte[] SignatureWithoutASM = CryptoProvider.RemoveASNHeaderFromSignature(SignedMessage);
        digitalSignature = new DigitalSignaturePacket(SignatureWithoutASM);
        Log.d(TAG, "Digital signature created.");
    }

    public static <TPacket> DeviceCredentialMessage<TPacket> forNfc(
        ReaderNoncePacket readerNonce,
        ProtocolVersionPacket protocolIdentifier)
    {
        byte[] toSign = readerNonce.encode();
        return new DeviceCredentialMessage<>(toSign, protocolIdentifier, null);
    }

    public static <TPacket> DeviceCredentialMessage<TPacket> forBleNormal(
        ReaderEphemeralPublicKeyPacket readerEphemeralPublicKey,
        ProtocolVersionPacket protocolIdentifier,
        LastUpdateTimePacket lastUpdateTime)
    {
        byte[] toSign = readerEphemeralPublicKey.encode();
        return new DeviceCredentialMessage<>(toSign, protocolIdentifier, lastUpdateTime);
    }

    public static <TPacket> DeviceCredentialMessage<TPacket> forBleEcdhe(
        SiteIdentifierPacket siteIdentifier,
        ReaderLocationIdentifierPacket readerLocationIdentifier,
        DeviceEphemeralPublicKeyPacket deviceEphemeralPublicKey,
        ReaderEphemeralPublicKeyPacket readerEphemeralPublicKey,
        ProtocolVersionPacket protocolIdentifier,
        LastUpdateTimePacket lastUpdateTime)
    {
        byte[] toSign = Arrays.concatenate(
            siteIdentifier.encode(),
            readerLocationIdentifier.encode(),
            deviceEphemeralPublicKey.getX(),
            readerEphemeralPublicKey.getX()
        );
        return new DeviceCredentialMessage<>(toSign, protocolIdentifier, lastUpdateTime);
    }

    private ValidationResult handlePublicKey(byte[] data)
    {
        Log.d(TAG, "handlePublicKey called.");
        var publicKeyPacket = new UncompressedPublicKeyPacket(data);
        ValidationResult vr = publicKeyPacket.validate();
        if (vr instanceof SuccessResult)
        {
            uncompressedPublicKey = publicKeyPacket;
            Log.i(TAG, "Public key packet processed successfully.");
        }
        else
        {
            Log.w(TAG, "Public key packet validation failed.");
        }
        return vr;
    }

    private ValidationResult handleDigitalSignature(byte[] data)
    {
        Log.d(TAG, "handleDigitalSignature called.");
        var digitalSignaturePacket = new DigitalSignaturePacket(data);
        ValidationResult vr = digitalSignaturePacket.validate();
        if (vr instanceof SuccessResult)
        {
            digitalSignature = digitalSignaturePacket;
            Log.i(TAG, "Digital signature packet processed successfully.");
        }
        else
        {
            Log.w(TAG, "Digital signature packet validation failed.");
        }
        return vr;
    }

    private ValidationResult handleLastUpdateTime(byte[] data)
    {
        Log.d(TAG, "handleLastUpdateTime called.");
        var lastUpdateTimePacket = new LastUpdateTimePacket(data);
        ValidationResult vr = lastUpdateTimePacket.validate();
        if (vr instanceof SuccessResult)
        {
            lastUpdateTime = lastUpdateTimePacket;
            Log.i(TAG, "Last update time packet processed successfully.");
        }
        else
        {
            Log.w(TAG, "Last update time packet validation failed.");
        }
        return vr;
    }

    public ValidationResult processNewPacket(TPacket packet)
    {
        Log.d(TAG, "processNewPacket called.");
        if (packet instanceof BLE_Packet)
        {
            var blePacket = (BLE_Packet)packet;
            Log.d(TAG, "Processing BLE Packet Type: " + blePacket.PacketType);
            switch (blePacket.PacketType)
            {
                case PublicKey:
                    return handlePublicKey(blePacket.Data);

                case DigitalSignature:
                    return handleDigitalSignature(blePacket.Data);

                case LastUpdateTime:
                    return handleLastUpdateTime(blePacket.Data);
            }
        }
        else if (packet instanceof NFC_Packet)
        {
            var nfcPacket = (NFC_Packet)packet;
            Log.d(TAG, "Processing NFC Packet Type: " + nfcPacket.PacketType);
            switch (nfcPacket.PacketType)
            {
                case UncompressedPublicKey:
                    return handlePublicKey(nfcPacket.Data);

                case DigitalSignature:
                    return handleDigitalSignature(nfcPacket.Data);
            }
        }

        Log.w(TAG, "Unexpected packet type received: " + packet.getClass().getSimpleName());
        return new UnexpectedPacketResult();
    }

    public ValidationResult validate()
    {
        Log.d(TAG, "validate called.");
        if (uncompressedPublicKey == null || digitalSignature == null)
        {
            Log.w(TAG, "Validation failed: Public key or signature is null.");
            return new ValidatedBeforeCompleteResult();
        }

        var isValid = CryptoProvider.validateSignedMessage(
            uncompressedPublicKey.encode(),
            digitalSignature.encode(),
            originalMessage);

        if (!isValid)
        {
            Log.e(TAG, "Signature validation failed.");
            return new InvalidSignatureResult();
        }

        Log.i(TAG, "Signature validation successful.");
        return new SuccessResult();
    }

    public byte[] encodePackets()
    {
        Log.d(TAG, "encodePackets called.");
        var uncompressedPublicKeyData = uncompressedPublicKey.encode();
        var signatureData = digitalSignature.encode();

        if (lastUpdateTime == null)
        {
            Log.d(TAG, "Encoding for NFC.");
            var pubKeyNfcTlv = TLVProvider.GetNfcTLV(NFC_PacketType.UncompressedPublicKey, uncompressedPublicKeyData);
            var signatureTlv = TLVProvider.GetNfcTLV(NFC_PacketType.DigitalSignature, signatureData);
            return Arrays.concatenate(pubKeyNfcTlv, signatureTlv);
        }

        Log.d(TAG, "Encoding for BLE.");
        var uncompressedPublicKeyTlv = TLVProvider.GetBleTLV(BLE_PacketType.PublicKey, uncompressedPublicKeyData);
        var signatureTlv = TLVProvider.GetBleTLV(BLE_PacketType.DigitalSignature, signatureData);

        var lastUpdateTimeData = lastUpdateTime.encode();
        var lastUpdateTimeTlv = TLVProvider.GetBleTLV(BLE_PacketType.LastUpdateTime, lastUpdateTimeData);
        var protocolIdentifierData = protocolIdentifier.encode();
        var protocolIdentifierTlv = TLVProvider.GetBleTLV(BLE_PacketType.ProtocolVersion, protocolIdentifierData);

        return Arrays.concatenate(uncompressedPublicKeyTlv, signatureTlv, lastUpdateTimeTlv, protocolIdentifierTlv);
    }
}
