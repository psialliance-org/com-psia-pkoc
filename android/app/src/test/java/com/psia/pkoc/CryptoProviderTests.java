package com.psia.pkoc;

import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.CCMBlockCipher;
import org.bouncycastle.crypto.modes.CCMModeCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
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

    private static byte[] encryptRef(byte[] key, byte[] nonce, byte[] msg)
    {
        CCMModeCipher ccm = CCMBlockCipher.newInstance(AESEngine.newInstance());
        AEADParameters params = new AEADParameters(new KeyParameter(key), CryptoProvider.CcmTagLength, nonce);
        ccm.init(true, params);

        byte[] out = new byte[ccm.getOutputSize(msg.length)];
        int n = ccm.processBytes(msg, 0, msg.length, out, 0);
        try {
            ccm.doFinal(out, n);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return out;
    }

    @Test
    public void ivFormat_bigEndianCounter()
    {
        int counter = 0x01020304;
        byte[] iv = CryptoProvider.getCcmIv(counter);

        assertEquals("CCM nonce must be 12 bytes here", 12, iv.length);

        ByteBuffer buf = ByteBuffer.wrap(iv, 8, 4).order(ByteOrder.BIG_ENDIAN);
        int parsed = buf.getInt();
        assertEquals("Last 4 bytes of IV should equal big-endian counter", counter, parsed);
    }

    @Test
    public void deterministic()
    {
        byte[] key = new byte[32];
        Arrays.fill(key, (byte) 0x11);

        byte[] msg = "PKOC-CCM-TEST".getBytes();
        int counter = 42;

        byte[] c1 = CryptoProvider.getAES256(key, msg, counter);
        byte[] c2 = CryptoProvider.getAES256(key, msg, counter);

        assertArrayEquals("Same inputs must yield same ciphertext", c1, c2);
    }

    @Test
    public void matchesReference()
    {
        byte[] key = new byte[32];
        for (int i = 0; i < key.length; i++) key[i] = (byte) (0xA0 + i);

        byte[] msg = "The quick brown fox".getBytes();
        int counter = 77;

        byte[] iv = CryptoProvider.getCcmIv(counter);
        byte[] expected = encryptRef(key, iv, msg);
        byte[] actual   = CryptoProvider.getAES256(key, msg, counter);

        assertArrayEquals("Ciphertext must match AES-CCM reference", expected, actual);
    }

    @Test
    public void outputLengthIncludesTag()
    {
        byte[] key = new byte[32];
        Arrays.fill(key, (byte) 0xAB);

        byte[] msg = new byte[25];
        Arrays.fill(msg, (byte) 0xCD);

        int counter = 7;
        int tagBytes = CryptoProvider.CcmTagLength / 8;

        byte[] out = CryptoProvider.getAES256(key, msg, counter);

        assertEquals("CCM output must be plaintext length + tag",
            msg.length + tagBytes, out.length);
    }

    @Test
    public void differentCountersChangeCiphertext()
    {
        byte[] key = new byte[32];
        Arrays.fill(key, (byte) 0x42);

        byte[] msg = "nonce matters".getBytes();

        byte[] c1 = CryptoProvider.getAES256(key, msg, 1);
        byte[] c2 = CryptoProvider.getAES256(key, msg, 2);

        assertNotEquals("Changing counter/IV must change ciphertext", c1, c2);
        assertNotEquals("IVs must differ for different counters", key, CryptoProvider.getCcmIv(1));
    }

    @Test(expected = RuntimeException.class)
    public void invalidKeyLengthThrows()
    {
        byte[] badKey = new byte[31]; // invalid for AES
        byte[] msg = "abc".getBytes();

        CryptoProvider.getAES256(badKey, msg, 5);
    }
}
