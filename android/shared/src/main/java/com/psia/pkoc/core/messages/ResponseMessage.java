package com.psia.pkoc.core.messages;

import com.psia.pkoc.core.BLE_Packet;
import com.psia.pkoc.core.BLE_PacketType;
import com.psia.pkoc.core.ReaderUnlockStatus;
import com.psia.pkoc.core.TLVProvider;
import com.psia.pkoc.core.ValidationResult;
import com.psia.pkoc.core.interfaces.TransactionMessage;
import com.psia.pkoc.core.packets.ResponsePacket;
import com.psia.pkoc.core.validations.SuccessResult;
import com.psia.pkoc.core.validations.UnexpectedPacketResult;
import com.psia.pkoc.core.validations.ValidatedBeforeCompleteResult;

public class ResponseMessage implements TransactionMessage<BLE_Packet, BLE_PacketType>
{
    private ResponsePacket responsePacket;

    public ResponseMessage()
    {
    }

    public ResponseMessage(ReaderUnlockStatus status)
    {
        responsePacket = new ResponsePacket(status);
    }

    public ResponsePacket getResponsePacket()
    {
        return responsePacket;
    }

    public ValidationResult processNewPacket(BLE_Packet packet)
    {
        if (packet.PacketType == BLE_PacketType.Response)
        {
            ResponsePacket obj = new ResponsePacket(packet.Data);
            var validationResult = obj.validate(packet.Data);
            if (validationResult instanceof SuccessResult)
            {
                responsePacket = obj;
                return new SuccessResult();
            }
            return validationResult;
        }

        return new UnexpectedPacketResult();
    }

    public ValidationResult validate()
    {
        if (responsePacket != null)
        {
            return new SuccessResult();
        }

        return new ValidatedBeforeCompleteResult();
    }

    public byte[] encodePackets()
    {
        var response = responsePacket.encode();
        return TLVProvider.GetBleTLV(BLE_PacketType.Response, response);
    }
}
