package com.psia.pkoc.core.messages;

import static com.psia.pkoc.core.BLE_PacketType.ReaderLocationIdentifier;

import com.psia.pkoc.core.BLE_Packet;
import com.psia.pkoc.core.BLE_PacketType;
import com.psia.pkoc.core.NFC_Packet;
import com.psia.pkoc.core.NFC_PacketType;
import com.psia.pkoc.core.TLVProvider;
import com.psia.pkoc.core.ValidationResult;
import com.psia.pkoc.core.interfaces.TransactionMessage;
import com.psia.pkoc.core.packets.ReaderNoncePacket;
import com.psia.pkoc.core.validations.SuccessResult;
import com.psia.pkoc.core.packets.ProtocolVersionPacket;
import com.psia.pkoc.core.packets.ReaderEphemeralPublicKeyPacket;
import com.psia.pkoc.core.packets.ReaderIdentifierPacket;
import com.psia.pkoc.core.packets.SiteIdentifierPacket;
import com.psia.pkoc.core.validations.UnexpectedPacketResult;
import com.psia.pkoc.core.validations.ValidatedBeforeCompleteResult;

import org.bouncycastle.util.Arrays;

public class ReaderIdentifierMessage<TPacket> implements TransactionMessage<TPacket>
{
    private boolean isNfc = false;
    private ProtocolVersionPacket protocolVersion;
    private ReaderEphemeralPublicKeyPacket compressedKey;
    private ReaderIdentifierPacket readerLocationId;
    private SiteIdentifierPacket siteId;
    private ReaderNoncePacket readerNonce;

    public ReaderIdentifierMessage()
    {
    }

    public ReaderIdentifierMessage(ReaderNoncePacket _readerNonce, ReaderIdentifierPacket _readerIdentifier)
    {
        readerNonce = _readerNonce;
        readerLocationId = _readerIdentifier;
    }

    public ReaderIdentifierMessage(ProtocolVersionPacket _protocolVersion, ReaderEphemeralPublicKeyPacket _compressedKey, ReaderIdentifierPacket _readerLocationId, SiteIdentifierPacket _siteId)
    {
        protocolVersion = _protocolVersion;
        compressedKey = _compressedKey;
        readerLocationId = _readerLocationId;
        siteId = _siteId;
    }

    public ProtocolVersionPacket getProtocolVersion()
    {
        return protocolVersion;
    }

    public ReaderEphemeralPublicKeyPacket getCompressedKey()
    {
        return compressedKey;
    }

    public ReaderIdentifierPacket getReaderLocationId()
    {
        return readerLocationId;
    }

    public SiteIdentifierPacket getSiteId()
    {
        return siteId;
    }

    public ReaderNoncePacket getReaderNonce()
    {
        return readerNonce;
    }

    private ValidationResult handleProtocolVersion(byte[] data)
    {
        var obj = new ProtocolVersionPacket(data);
        ValidationResult vr = obj.validate();
        if (vr instanceof SuccessResult)
        {
            protocolVersion = obj;
        }
        return vr;
    }

    private ValidationResult handlePublicKey(byte[] data)
    {
        var obj = new ReaderEphemeralPublicKeyPacket(data);
        ValidationResult vr = obj.validate();
        if (vr instanceof SuccessResult)
        {
            compressedKey = obj;
        }
        return vr;
    }

    public ValidationResult handleReaderIdentifier(byte[] data)
    {
        var obj = new ReaderIdentifierPacket(data);
        ValidationResult vr = obj.validate();
        if (vr instanceof SuccessResult)
        {
            readerLocationId = obj;
        }
        return vr;
    }

    public ValidationResult handleSiteIdentifier(byte[] data)
    {
        var obj = new SiteIdentifierPacket(data);
        ValidationResult vr = obj.validate();
        if (vr instanceof SuccessResult)
        {
            siteId = obj;
        }
        return vr;
    }

    private ValidationResult handleReaderNonce(byte[] data)
    {
        var obj = new ReaderNoncePacket(data);
        ValidationResult vr = obj.validate();
        if (vr instanceof SuccessResult)
        {
            readerNonce = obj;
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
                case ProtocolVersion:
                    return handleProtocolVersion(blePacket.Data);

                case CompressedTransientPublicKey:
                    return handlePublicKey(blePacket.Data);

                case ReaderLocationIdentifier:
                    return handleReaderIdentifier(blePacket.Data);

                case SiteIdentifier:
                    return handleSiteIdentifier(blePacket.Data);
            }
        }
        else if (packet instanceof NFC_Packet)
        {
            isNfc = true;
            var nfcPacket = (NFC_Packet)packet;
            switch (nfcPacket.PacketType)
            {
                case ProtocolVersion:
                    return handleProtocolVersion(nfcPacket.Data);

                case TransactionIdentifier:
                    return handleReaderNonce(nfcPacket.Data);

                case ReaderIdentifier:
                    return handleReaderIdentifier(nfcPacket.Data);
            }
        }

        return vr;
    }

    @Override
    public ValidationResult validate()
    {
        if (getReaderNonce() == null)
        {
            if (protocolVersion == null || readerLocationId == null || siteId == null || compressedKey == null)
            {
                return new ValidatedBeforeCompleteResult();
            }
        }
        else
        {
            if (readerLocationId == null || siteId == null)
            {
                return new ValidatedBeforeCompleteResult();
            }
        }

        return new SuccessResult();
    }

    @Override
    public byte[] encodePackets()
    {
        var protocolVersionData = protocolVersion.encode();
        var readerLocationIdData = readerLocationId.encode();

        if (isNfc)
        {
            var readerNonceData = readerNonce.encode();
            var protocolVersionTlv = TLVProvider.GetNfcTLV(NFC_PacketType.ProtocolVersion, protocolVersionData);
            var readerNonceTlv = TLVProvider.GetNfcTLV(NFC_PacketType.TransactionIdentifier, readerNonceData);
            var readerLocationIdTlv = TLVProvider.GetNfcTLV(NFC_PacketType.ReaderIdentifier, readerLocationIdData);
            return Arrays.concatenate(protocolVersionTlv, readerNonceTlv, readerLocationIdTlv);
        }

        var protocolVersionTlv = TLVProvider.GetBleTLV(BLE_PacketType.ProtocolVersion, protocolVersionData);
        var compressedKeyData = compressedKey.encode();
        var compressedKeyTlv = TLVProvider.GetBleTLV(BLE_PacketType.CompressedTransientPublicKey, compressedKeyData);
        var readerLocationIdTlv = TLVProvider.GetBleTLV(ReaderLocationIdentifier, readerLocationIdData);
        var siteIdData = siteId.encode();
        var siteIdTlv = TLVProvider.GetBleTLV(BLE_PacketType.SiteIdentifier, siteIdData);

        return Arrays.concatenate(protocolVersionTlv, compressedKeyTlv, readerLocationIdTlv, siteIdTlv);
    }
}
