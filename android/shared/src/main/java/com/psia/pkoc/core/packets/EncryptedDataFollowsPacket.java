package com.psia.pkoc.core.packets;

import com.psia.pkoc.core.ValidationResult;
import com.psia.pkoc.core.interfaces.TransactionPacket;
import com.psia.pkoc.core.validations.SuccessResult;
import com.psia.pkoc.core.validations.UnexpectedPacketResult;

public class EncryptedDataFollowsPacket implements TransactionPacket
{
    private final byte[] encryptedData;

    public EncryptedDataFollowsPacket(byte[] data)
    {
        encryptedData = data;
    }

    @Override
    public byte[] encode()
    {
        return encryptedData;
    }

    @Override
    public ValidationResult validate()
    {
        if (encryptedData.length > 0)
        {
            return new SuccessResult();
        }

        return new UnexpectedPacketResult();
    }
}
