package com.psia.pkoc.core.packets;

import com.psia.pkoc.core.ValidationResult;
import com.psia.pkoc.core.interfaces.TransactionPacket;
import com.psia.pkoc.core.validations.SuccessResult;

public class SiteIdentifierPacket implements TransactionPacket
{
    private final byte[] identifier;

    public SiteIdentifierPacket(byte[] data)
    {
        identifier = data;
    }

    @Override
    public byte[] encode()
    {
        return identifier;
    }

    @Override
    public ValidationResult validate()
    {
        return new SuccessResult();
    }
}
