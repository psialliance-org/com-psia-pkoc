package com.psia.pkoc.core.transactions;

import com.psia.pkoc.core.BLE_Packet;
import com.psia.pkoc.core.BLE_PacketType;
import com.psia.pkoc.core.TLVProvider;
import com.psia.pkoc.core.ValidationResult;
import com.psia.pkoc.core.validations.SuccessResult;

public class BleNormalFlowTransaction extends NormalFlowTransaction<BLE_Packet, BLE_PacketType>
{
    public BleNormalFlowTransaction(boolean _isDevice)
    {
        super(_isDevice);
    }

    @Override
    public ValidationResult processNewData(byte[] data)
    {
        var packets = TLVProvider.GetBleValues(data);
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
