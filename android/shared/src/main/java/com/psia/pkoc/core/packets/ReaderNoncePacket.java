package com.psia.pkoc.core.packets;

import com.psia.pkoc.core.ValidationResult;
import com.psia.pkoc.core.interfaces.TransactionPacket;
import com.psia.pkoc.core.validations.SizeMismatchResult;
import com.psia.pkoc.core.validations.SuccessResult;

public class ReaderNoncePacket implements TransactionPacket
{
    private final byte[] nonce;

    public ReaderNoncePacket(byte[] data)
    {
        nonce = data;
    }

    @Override
    public byte[] encode()
    {
        return nonce;
    }

    @Override
    public ValidationResult validate()
    {
        var sizeValidation = new SizeMismatchResult(nonce.length, 16, 65);
        if (sizeValidation.isValid == false)
        {
            return sizeValidation;
        }

        return new SuccessResult();
    }
}
