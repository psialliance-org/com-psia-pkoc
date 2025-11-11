package com.psia.pkoc.core.packets;

import com.psia.pkoc.core.ValidationResult;
import com.psia.pkoc.core.interfaces.TransactionPacket;
import com.psia.pkoc.core.validations.SuccessResult;

import org.bouncycastle.util.Arrays;

public class DeviceEphemeralPublicKeyPacket implements TransactionPacket
{
    private final byte[] key;

    public DeviceEphemeralPublicKeyPacket(byte[] data)
    {
        key = data;
    }

    @Override
    public byte[] encode()
    {
        return key;
    }

    @Override
    public ValidationResult validate()
    {
        return new SuccessResult();
    }

    public byte[] getX()
    {
        return Arrays.copyOfRange(key, 1, 33);
    }
}
