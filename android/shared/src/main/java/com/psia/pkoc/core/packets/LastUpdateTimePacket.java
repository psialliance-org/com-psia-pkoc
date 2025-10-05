package com.psia.pkoc.core.packets;

import com.psia.pkoc.core.ValidationResult;
import com.psia.pkoc.core.interfaces.TransactionPacket;
import com.psia.pkoc.core.validations.SizeMismatchResult;
import com.psia.pkoc.core.validations.SuccessResult;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class LastUpdateTimePacket implements TransactionPacket
{
    private final byte[] epochSecondsBE4;

    public LastUpdateTimePacket (byte[] epochSecondsBigEndian4)
    {
        if (epochSecondsBigEndian4 == null)
        {
            throw new IllegalArgumentException("epochSecondsBigEndian4 is null");
        }
        if (epochSecondsBigEndian4.length != 4)
        {
            throw new IllegalArgumentException("LastUpdateTime requires 4 bytes");
        }
        epochSecondsBE4 = Arrays.copyOf(epochSecondsBigEndian4, 4);
    }

    public LastUpdateTimePacket (int unixEpochSeconds)
    {
        var buf = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN);
        buf.putInt(unixEpochSeconds);
        epochSecondsBE4 = buf.array();
    }

    public byte[] encode()
    {
        return Arrays.copyOf(epochSecondsBE4, 4);
    }

    public ValidationResult validate(byte[] data)
    {
        var size = new SizeMismatchResult(data.length, 4);
        if (size.isValid == false)
        {
            return size;
        }

        return new SuccessResult();
    }
}
