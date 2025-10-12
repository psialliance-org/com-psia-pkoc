package com.psia.pkoc.core.packets;

import com.psia.pkoc.core.ReaderUnlockStatus;
import com.psia.pkoc.core.ValidationResult;
import com.psia.pkoc.core.interfaces.TransactionPacket;
import com.psia.pkoc.core.validations.SizeMismatchResult;
import com.psia.pkoc.core.validations.SuccessResult;
import com.psia.pkoc.core.validations.UnexpectedPacketResult;

import java.math.BigInteger;

public class ResponsePacket implements TransactionPacket
{
    private final ReaderUnlockStatus readerUnlockStatus;

    public ResponsePacket(byte[] data)
    {
        readerUnlockStatus = ReaderUnlockStatus.values()[data[0]];
    }

    public ResponsePacket(ReaderUnlockStatus status)
    {
        readerUnlockStatus = status;
    }

    public ReaderUnlockStatus getReaderUnlockStatus()
    {
        return readerUnlockStatus;
    }

    @Override
    public byte[] encode()
    {
        return new byte[]
        {
            BigInteger.valueOf(ReaderUnlockStatus.AccessGranted.ordinal()).byteValue()
        };
    }

    @Override
    public ValidationResult validate()
    {
        var sizeMismatchResult = new SizeMismatchResult(encode().length, 1);
        if (!sizeMismatchResult.isValid)
        {
            return sizeMismatchResult;
        }

        if (encode()[0] >= ReaderUnlockStatus.values().length)
        {
            return new UnexpectedPacketResult();
        }

        return new SuccessResult();
    }
}
