package com.psia.pkoc.core.interfaces;

import com.psia.pkoc.core.ValidationResult;

public interface TransactionPacket
{
    byte[] encode();
    public ValidationResult validate(byte[] data);
}
