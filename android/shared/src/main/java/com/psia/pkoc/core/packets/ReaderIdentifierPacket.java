package com.psia.pkoc.core.packets;

import com.psia.pkoc.core.ValidationResult;
import com.psia.pkoc.core.interfaces.TransactionPacket;
import com.psia.pkoc.core.validations.SizeMismatchResult;
import com.psia.pkoc.core.validations.SuccessResult;

import java.util.Arrays;

public class ReaderIdentifierPacket implements TransactionPacket
{
    private final byte[] readerLocationGuid16;

    public ReaderIdentifierPacket(byte[] data)
    {
        readerLocationGuid16 = Arrays.copyOf(data, data.length);
    }

    public byte[] getReaderLocationIdentifier()
    {
        return Arrays.copyOf(readerLocationGuid16, readerLocationGuid16.length);
    }

    public byte[] encode()
    {
        return Arrays.copyOf(readerLocationGuid16, readerLocationGuid16.length);
    }

    public ValidationResult validate()
    {
        var expectedSizes = new int[]{16, 32};
        var sizeMismatch = new SizeMismatchResult(readerLocationGuid16.length, expectedSizes);
        if (sizeMismatch.isValid == false)
        {
            return sizeMismatch;
        }

        return new SuccessResult();
    }
}
