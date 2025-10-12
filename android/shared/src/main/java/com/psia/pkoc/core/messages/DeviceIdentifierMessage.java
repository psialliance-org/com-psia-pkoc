package com.psia.pkoc.core.messages;

import com.psia.pkoc.core.BLE_Packet;
import com.psia.pkoc.core.BLE_PacketType;
import com.psia.pkoc.core.TLVProvider;
import com.psia.pkoc.core.ValidationResult;
import com.psia.pkoc.core.interfaces.TransactionMessage;
import com.psia.pkoc.core.packets.DeviceEphemeralPublicKeyPacket;
import com.psia.pkoc.core.packets.ProtocolVersionPacket;
import com.psia.pkoc.core.validations.SuccessResult;
import com.psia.pkoc.core.validations.UnexpectedPacketResult;
import com.psia.pkoc.core.validations.ValidatedBeforeCompleteResult;

import org.bouncycastle.util.Arrays;

public class DeviceIdentifierMessage implements TransactionMessage<BLE_Packet>
{
    private DeviceEphemeralPublicKeyPacket deviceEphemeralPublicKey;
    private ProtocolVersionPacket protocolVersionPacket;

    public DeviceIdentifierMessage (DeviceEphemeralPublicKeyPacket _deviceEphemeralPublicKey, ProtocolVersionPacket _protocolVersionPacket)
    {
        deviceEphemeralPublicKey = _deviceEphemeralPublicKey;
        protocolVersionPacket = _protocolVersionPacket;
    }

    @Override
    public byte[] encodePackets()
    {
        var deviceEphemeralPublicKeyData = deviceEphemeralPublicKey.encode();
        var protocolVersionData = protocolVersionPacket.encode();
        var deviceEphemeralPublicKeyTlv = TLVProvider.GetBleTLV(BLE_PacketType.UncompressedTransientPublicKey, deviceEphemeralPublicKeyData);
        var protocolVersionTlv = TLVProvider.GetBleTLV(BLE_PacketType.ProtocolVersion, protocolVersionData);
        return Arrays.concatenate(deviceEphemeralPublicKeyTlv, protocolVersionTlv);
    }

    private ValidationResult handleProtocolVersion(byte[] data)
    {
        var obj = new ProtocolVersionPacket(data);
        ValidationResult vr = obj.validate();
        if (vr instanceof SuccessResult)
        {
            protocolVersionPacket = obj;
        }
        return vr;
    }

    private ValidationResult handlePublicKey(byte[] data)
    {
        var obj = new DeviceEphemeralPublicKeyPacket(data);
        ValidationResult vr = obj.validate();
        if (vr instanceof SuccessResult)
        {
            deviceEphemeralPublicKey = obj;
        }
        return vr;
    }

    @Override
    public ValidationResult processNewPacket(BLE_Packet blePacket)
    {
        ValidationResult vr = new UnexpectedPacketResult();

        switch (blePacket.PacketType)
        {
            case ProtocolVersion:
                return handleProtocolVersion(blePacket.Data);

            case UncompressedTransientPublicKey:
                return handlePublicKey(blePacket.Data);
        }
        return vr;
    }

    @Override
    public ValidationResult validate()
    {
        if (protocolVersionPacket == null || deviceEphemeralPublicKey == null)
        {
            return new ValidatedBeforeCompleteResult();
        }

        return new SuccessResult();
    }
}
