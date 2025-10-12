package com.psia.pkoc.core.packets;

import com.psia.pkoc.core.ValidationResult;
import com.psia.pkoc.core.interfaces.TransactionPacket;
import com.psia.pkoc.core.validations.SuccessResult;

public class ReaderLocationIdentifierPacket implements TransactionPacket
{
    private final byte[] identifier;

    public ReaderLocationIdentifierPacket(byte[] data)
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
