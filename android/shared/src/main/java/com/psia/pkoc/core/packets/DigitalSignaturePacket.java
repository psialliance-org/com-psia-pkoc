package com.psia.pkoc.core.packets;

import com.psia.pkoc.core.ValidationResult;
import com.psia.pkoc.core.interfaces.TransactionPacket;
import com.psia.pkoc.core.validations.SizeMismatchResult;
import com.psia.pkoc.core.validations.SuccessResult;

import java.util.Arrays;

public class DigitalSignaturePacket implements TransactionPacket
{
    private final byte[] signature64;

    public DigitalSignaturePacket(byte[] signatureRConcatS64)
    {
        signature64 = Arrays.copyOf(signatureRConcatS64, signatureRConcatS64.length);
    }

    public byte[] encode()
    {
        return Arrays.copyOf(signature64, signature64.length);
    }

    public ValidationResult validate(byte[] data)
    {
        var size = new SizeMismatchResult(data.length, 64);
        if (size.isValid == false)
        {
            return size;
        }

        return new SuccessResult();
    }
}
