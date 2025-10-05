package com.psia.pkoc.core.packets;

import com.psia.pkoc.core.ValidationResult;
import com.psia.pkoc.core.interfaces.TransactionPacket;
import com.psia.pkoc.core.validations.SizeMismatchResult;
import com.psia.pkoc.core.validations.SuccessResult;

import java.util.Arrays;

public class SiteIdentifierPacket implements TransactionPacket
{
    private final byte[] siteIdentifierGuid16;

    public SiteIdentifierPacket(byte[] data)
    {
        siteIdentifierGuid16 = Arrays.copyOf(data, data.length);
    }

    public byte[] getSiteIdentifier()
    {
        return Arrays.copyOf(siteIdentifierGuid16, siteIdentifierGuid16.length);
    }

    @Override
    public byte[] encode()
    {
        return Arrays.copyOf(siteIdentifierGuid16, siteIdentifierGuid16.length);
    }

    @Override
    public ValidationResult validate(byte[] data)
    {
        var sizeMismatch = new SizeMismatchResult(data.length, 16);
        if (sizeMismatch.isValid == false)
        {
            return sizeMismatch;
        }

        return new SuccessResult();
    }
}
