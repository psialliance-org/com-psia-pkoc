package com.psia.pkoc;

import android.text.TextUtils;

import androidx.annotation.Nullable;

import java.util.UUID;

public final class Validators
{
    public static boolean isValidUuid(@Nullable String s)
    {
        if(TextUtils.isEmpty(s))
        {
            return true;
        }

        try
        {
            UUID.fromString(s);
            return false;
        }
        catch (IllegalArgumentException ignored)
        {
            return true;
        }
    }

    /** expectedBytes = 65 for uncompressed EC pubkey */
    public static boolean isValidHex(@Nullable String s, int expectedBytes)
    {
        if (TextUtils.isEmpty(s))
        {
            return false;
        }

        if (s.length() != expectedBytes * 2)
        {
            return false;
        }

        return s.matches("^[0-9A-Fa-f]+$");
    }
}

