package com.psia.pkoc.core.messages;

import com.psia.pkoc.core.BLE_Packet;
import com.psia.pkoc.core.BLE_PacketType;
import com.psia.pkoc.core.CryptoProvider;
import com.psia.pkoc.core.NFC_Packet;
import com.psia.pkoc.core.NFC_PacketType;
import com.psia.pkoc.core.TLVProvider;
import com.psia.pkoc.core.ValidationResult;
import com.psia.pkoc.core.interfaces.TransactionMessage;
import com.psia.pkoc.core.packets.DigitalSignaturePacket;
import com.psia.pkoc.core.packets.LastUpdateTimePacket;
import com.psia.pkoc.core.packets.ProtocolVersionPacket;
import com.psia.pkoc.core.packets.UncompressedPublicKeyPacket;
import com.psia.pkoc.core.validations.InvalidSignatureResult;
import com.psia.pkoc.core.validations.SuccessResult;
import com.psia.pkoc.core.validations.UnexpectedPacketResult;
import com.psia.pkoc.core.validations.ValidatedBeforeCompleteResult;

import org.bouncycastle.util.Arrays;

public class DeviceCredentialMessage<TPacket, TPacketType> implements TransactionMessage<TPacket, TPacketType>
{
    private final byte[] originalMessage;
    private UncompressedPublicKeyPacket uncompressedPublicKey;
    private DigitalSignaturePacket digitalSignature;
    private LastUpdateTimePacket lastUpdateTime;
    private final ProtocolVersionPacket protocolIdentifier;

    public DeviceCredentialMessage(byte[] toSign, ProtocolVersionPacket _protocolIdentifier)
    {
        //lastUpdateTime = _lastUpdateTime;
        protocolIdentifier = _protocolIdentifier;

        var publicKey = CryptoProvider.getUncompressedPublicKeyBytes();
        uncompressedPublicKey = new UncompressedPublicKeyPacket(publicKey);

        originalMessage = toSign;
        byte[] SignedMessage = CryptoProvider.GetSignedMessage(toSign);
        byte[] SignatureWithoutASM = CryptoProvider.RemoveASNHeaderFromSignature(SignedMessage);
        digitalSignature = new DigitalSignaturePacket(SignatureWithoutASM);
    }

    private ValidationResult handlePublicKey(byte[] data)
    {
        var publicKeyPacket = new UncompressedPublicKeyPacket(data);
        ValidationResult vr = publicKeyPacket.validate(data);
        if (vr instanceof SuccessResult)
        {
            uncompressedPublicKey = publicKeyPacket;
        }
        return vr;
    }

    private ValidationResult handleDigitalSignature(byte[] data)
    {
        var digitalSignaturePacket = new DigitalSignaturePacket(data);
        ValidationResult vr = digitalSignaturePacket.validate(data);
        if (vr instanceof SuccessResult)
        {
            digitalSignature = digitalSignaturePacket;
        }
        return vr;
    }

    private ValidationResult handleLastUpdateTime(byte[] data)
    {
        var lastUpdateTimePacket = new LastUpdateTimePacket(data);
        ValidationResult vr = lastUpdateTimePacket.validate(data);
        if (vr instanceof SuccessResult)
        {
            lastUpdateTime = lastUpdateTimePacket;
        }
        return vr;
    }

    public ValidationResult processNewPacket(TPacket packet)
    {
        ValidationResult vr = new UnexpectedPacketResult();

        if (packet instanceof BLE_Packet)
        {
            var blePacket = (BLE_Packet)packet;
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
            switch (nfcPacket.PacketType)
            {
                case UncompressedPublicKey:
                    return handlePublicKey(nfcPacket.Data);

                case DigitalSignature:
                    return handleDigitalSignature(nfcPacket.Data);
            }

        }

        return vr;
    }

    public ValidationResult validate()
    {
        if (uncompressedPublicKey == null || digitalSignature == null)
        {
            return new ValidatedBeforeCompleteResult();
        }

        var isValid = CryptoProvider.validateSignedMessage(
            uncompressedPublicKey.encode(),
            digitalSignature.encode(),
            originalMessage);

        if (!isValid)
        {
            return new InvalidSignatureResult();
        }

        return new SuccessResult();
    }

    public byte[] encodePackets()
    {
        var uncompressedPublicKeyData = uncompressedPublicKey.encode();
        var signatureData = digitalSignature.encode();

        if (lastUpdateTime == null)
        {
            var pubKeyNfcTlv = TLVProvider.GetNfcTLV(NFC_PacketType.UncompressedPublicKey, uncompressedPublicKeyData);
            var signatureTlv = TLVProvider.GetNfcTLV(NFC_PacketType.DigitalSignature, signatureData);
            return Arrays.concatenate(pubKeyNfcTlv, signatureTlv);
        }

        var uncompressedPublicKeyTlv = TLVProvider.GetBleTLV(BLE_PacketType.PublicKey, uncompressedPublicKeyData);
        var signatureTlv = TLVProvider.GetBleTLV(BLE_PacketType.DigitalSignature, signatureData);

        var lastUpdateTimeData = lastUpdateTime.encode();
        var lastUpdateTimeTlv = TLVProvider.GetBleTLV(BLE_PacketType.LastUpdateTime, lastUpdateTimeData);
        var protocolIdentifierData = protocolIdentifier.encode();
        var protocolIdentifierTlv = TLVProvider.GetBleTLV(BLE_PacketType.ProtocolVersion, protocolIdentifierData);

        return Arrays.concatenate(uncompressedPublicKeyTlv, signatureTlv, lastUpdateTimeTlv, protocolIdentifierTlv);
    }
}
