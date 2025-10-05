package com.psia.pkoc.core.packets;

import com.psia.pkoc.core.ValidationResult;
import com.psia.pkoc.core.interfaces.TransactionPacket;
import com.psia.pkoc.core.validations.IncorrectKeyPrefixResult;
import com.psia.pkoc.core.validations.SizeMismatchResult;
import com.psia.pkoc.core.validations.SuccessResult;

import java.util.Arrays;

public class CompressedTransientPublicKeyPacket implements TransactionPacket
{
    private final byte[] compressedKey33;

    public CompressedTransientPublicKeyPacket(byte[] data)
    {
        compressedKey33 = Arrays.copyOf(data, data.length);
    }

    public byte[] encode()
    {
        return Arrays.copyOf(compressedKey33, compressedKey33.length);
    }

    public ValidationResult validate(byte[] data)
    {
        var sizeMismatch = new SizeMismatchResult(data.length, 33);
        if (sizeMismatch.isValid == false)
        {
            return sizeMismatch;
        }

        int prefix = compressedKey33[0] & 0xFF;
        boolean validPrefix = prefix == 0x02 || prefix == 0x03;
        if (!validPrefix)
        {
            return new IncorrectKeyPrefixResult();
        }

        return new SuccessResult();
    }
}
