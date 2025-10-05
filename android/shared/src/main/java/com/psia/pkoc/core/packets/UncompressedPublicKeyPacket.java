package com.psia.pkoc.core.packets;

import com.psia.pkoc.core.ValidationResult;
import com.psia.pkoc.core.interfaces.TransactionPacket;
import com.psia.pkoc.core.validations.IncorrectKeyPrefixResult;
import com.psia.pkoc.core.validations.SizeMismatchResult;
import com.psia.pkoc.core.validations.SuccessResult;

import java.util.Arrays;

public class UncompressedPublicKeyPacket implements TransactionPacket
{
    private final byte[] key65;

    public UncompressedPublicKeyPacket(byte[] uncompressedKey65)
    {
        key65 = Arrays.copyOf(uncompressedKey65, uncompressedKey65.length);
    }

    @Override
    public byte[] encode()
    {
        return Arrays.copyOf(key65, key65.length);
    }

    @Override
    public ValidationResult validate(byte[] data)
    {
        var size = new SizeMismatchResult(data.length, 65);
        if (size.isValid == false)
        {
            return size;
        }

        boolean startsWith04 = (data[0] & 0xFF) == 0x04;
        if (!startsWith04)
        {
            return new IncorrectKeyPrefixResult();
        }

        return new SuccessResult();
    }
}
