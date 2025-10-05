package com.psia.pkoc.core.transactions;

import com.psia.pkoc.core.NFC_Packet;
import com.psia.pkoc.core.NFC_PacketType;
import com.psia.pkoc.core.TLVProvider;
import com.psia.pkoc.core.ValidationResult;
import com.psia.pkoc.core.validations.SuccessResult;

public class NfcNormalFlowTransaction extends NormalFlowTransaction<NFC_Packet, NFC_PacketType>
{
    public NfcNormalFlowTransaction(boolean _isDevice)
    {
        super(_isDevice);
    }

    @Override
    public ValidationResult processNewData(byte[] data)
    {
        var packets = TLVProvider.GetNfcValues(data);
        for (var packet : packets)
        {
            var vr = processNewPacket(packet);
            if (!vr.isValid)
            {
                return vr;
            }
        }

        return new SuccessResult();
    }
}
