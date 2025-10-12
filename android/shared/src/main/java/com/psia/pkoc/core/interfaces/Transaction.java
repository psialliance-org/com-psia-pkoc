package com.psia.pkoc.core.interfaces;

import com.psia.pkoc.core.ReaderUnlockStatus;
import com.psia.pkoc.core.ValidationResult;

public interface Transaction
{
    public ReaderUnlockStatus getReaderUnlockStatus();
    public ValidationResult processNewData(byte[] packets);
    public byte[] toWrite();
}
