package com.psia.pkoc.core.packets;

import com.psia.pkoc.core.ValidationResult;
import com.psia.pkoc.core.interfaces.TransactionPacket;
import com.psia.pkoc.core.validations.SizeMismatchResult;
import com.psia.pkoc.core.validations.SuccessResult;

public class NfcProtocolVersionPacket implements TransactionPacket
{
    private final byte[] version = new byte[]{ 0x01, 0x00 };

    public NfcProtocolVersionPacket()
    {
    }

    @Override
    public byte[] encode()
    {
        return version;
    }

    @Override
    public ValidationResult validate()
    {
        var sizeValidation = new SizeMismatchResult(version.length, 2, 2);
        if (sizeValidation.isValid == false)
        {
            return sizeValidation;
        }

        return new SuccessResult();
    }
}
