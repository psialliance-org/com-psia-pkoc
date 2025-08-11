package com.psia.pkoc;

import org.junit.Test;
import static org.junit.Assert.*;

public final class CryptoProviderTests
{
    private static final byte[] PREFIX = new byte[] {0,0,0,0,0,0,0,1};

    @Test
    public void iv_is12Bytes()
    {
        byte[] iv = CryptoProvider.getCcmIv(1);
        assertEquals(12, iv.length);
    }

    @Test
    public void iv_hasCorrectPrefix()
    {
        byte[] iv = CryptoProvider.getCcmIv(1);
        for (int i = 0; i < 8; i++)
        {
            assertEquals(PREFIX[i], iv[i]);
        }
    }

    @Test
    public void counter_bigEndian_forOne()
    {
        byte[] iv = CryptoProvider.getCcmIv(1);
        assertArrayEquals(new byte[]{0,0,0,1}, slice(iv));
    }

    @Test
    public void counter_bigEndian_forArbitrary()
    {
        byte[] iv = CryptoProvider.getCcmIv(0x12_34_56_78);
        assertArrayEquals(new byte[]{0x12,0x34,0x56,0x78}, slice(iv));
    }

    @Test
    public void counter_maxUnsigned()
    {
        byte[] iv = CryptoProvider.getCcmIv(-1);
        assertArrayEquals(new byte[]{(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF}, slice(iv));
    }

    @Test
    public void differentCounters_produceDifferentIvs()
    {
        assertFalse(java.util.Arrays.equals(CryptoProvider.getCcmIv(1), CryptoProvider.getCcmIv(2)));
    }

    private static byte[] slice(byte[] src)
    {
        byte[] out = new byte[12 - 8];
        System.arraycopy(src, 8, out, 0, out.length);
        return out;
    }
}
