package com.psia.pkoc.core.transactions;

import com.psia.pkoc.core.ValidationResult;
import com.psia.pkoc.core.interfaces.Transaction;
import com.psia.pkoc.core.interfaces.TransactionMessage;
import com.psia.pkoc.core.messages.DeviceCredentialMessage;
import com.psia.pkoc.core.messages.ReaderIdentifierMessage;
import com.psia.pkoc.core.packets.CompressedTransientPublicKeyPacket;
import com.psia.pkoc.core.packets.ProtocolVersionPacket;
import com.psia.pkoc.core.packets.ReaderIdentifierPacket;
import com.psia.pkoc.core.packets.SiteIdentifierPacket;
import com.psia.pkoc.core.validations.SuccessResult;
import com.psia.pkoc.core.validations.UnexpectedPacketResult;

public class NormalFlowTransaction<TPacket, TType> implements Transaction
{
    private boolean isDevice;
    private byte[] toWrite;

    private TransactionMessage<TPacket, TType> currentMessage;

    public NormalFlowTransaction(boolean _isDevice)
    {
        currentMessage = new ReaderIdentifierMessage<>();
        isDevice = _isDevice;
    }

    public NormalFlowTransaction(ProtocolVersionPacket _protocolVersion,
                                    CompressedTransientPublicKeyPacket _compressedKey,
                                    ReaderIdentifierPacket _readerLocationId,
                                    SiteIdentifierPacket _siteId)
    {
        currentMessage = new ReaderIdentifierMessage<>(_protocolVersion, _compressedKey, _readerLocationId, _siteId);
        toWrite = currentMessage.encodePackets();
    }

    public ValidationResult processNewPacket(TPacket packet)
    {
        if (currentMessage instanceof ReaderIdentifierMessage)
        {
            var readerIdentifierMessage = (ReaderIdentifierMessage<TPacket, TType>)currentMessage;
            if (isDevice)
            {
                ValidationResult vr = readerIdentifierMessage.processNewPacket(packet);
                var messageValidation = readerIdentifierMessage.validate();
                if (!vr.isValid)
                {
                    return messageValidation;
                }
                else
                {
                    if (messageValidation.isValid)
                    {
                        byte[] toSign = new byte[0];
                        if (readerIdentifierMessage.getCompressedKey() != null)
                        {
                            toSign = readerIdentifierMessage
                                .getCompressedKey()
                                .encode();
                        }
                        else if (readerIdentifierMessage.getReaderNonce() != null)
                        {
                            toSign = readerIdentifierMessage
                                .getReaderNonce()
                                .encode();
                        }
                        currentMessage = new DeviceCredentialMessage<>(toSign,
                            readerIdentifierMessage.getProtocolVersion()
                        );
                        toWrite = currentMessage.encodePackets();
                    }
                    else if (messageValidation.cancelTransaction)
                    {
                        return messageValidation;
                    }
                }
                return vr;
            }
        }

        return new UnexpectedPacketResult();
    }

    @Override
    public ValidationResult processNewData(byte[] data)
    {
        return new SuccessResult();
    }

    @Override
    public byte[] toWrite()
    {
        byte[] toReturn = toWrite.clone();
        toWrite = null;
        return toReturn;
    }
}