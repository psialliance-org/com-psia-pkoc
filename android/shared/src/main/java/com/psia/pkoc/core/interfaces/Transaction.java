package com.psia.pkoc.core.interfaces;

import com.psia.pkoc.core.ValidationResult;

public interface Transaction
{
    public ValidationResult processNewData(byte[] packets);
    public byte[] toWrite();
}
