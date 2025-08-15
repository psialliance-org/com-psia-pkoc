package com.psia.pkoc;

import androidx.room.TypeConverter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

public final class UuidConverters
{
    @TypeConverter
    public static byte[] fromUuid(UUID uuid)
    {
        if (uuid == null)
        {
            return null;
        }
        ByteBuffer buf = ByteBuffer.allocate(16).order(ByteOrder.BIG_ENDIAN);
        buf.putLong(uuid.getMostSignificantBits());
        buf.putLong(uuid.getLeastSignificantBits());
        return buf.array();
    }

    @TypeConverter
    public static UUID toUuid(byte[] bytes)
    {
        if (bytes == null)
        {
            return null;
        }
        if (bytes.length != 16)
        {
            throw new IllegalArgumentException("UUID BLOB must be 16 bytes; got " + bytes.length);
        }
        ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
        long msb = buf.getLong();
        long lsb = buf.getLong();
        return new UUID(msb, lsb);
    }
}
