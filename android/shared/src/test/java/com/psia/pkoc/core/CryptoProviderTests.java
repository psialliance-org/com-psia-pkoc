package com.psia.pkoc.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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
    public void decryptsValidCiphertext_matchesReference()
    {
        byte[] key = new byte[32];
        for (int i = 0; i < key.length; i++) key[i] = (byte) (0x20 + i);

        byte[] msg = "PKOC CCM decryption test".getBytes();
        int counter = 123;

        byte[] iv = CryptoProvider.getCcmIv(counter);
        byte[] ct = encryptRef(key, iv, msg);

        byte[] pt = CryptoProvider.getFromAES256(key, ct, counter);

        assertNotNull("Expected successful decryption", pt);
        assertArrayEquals("Plaintext must match original", msg, pt);
        assertEquals("Output length equals plaintext length", msg.length, pt.length);
    }

    @Test
    public void wrongKey_returnsNull()
    {
        byte[] keyEnc = new byte[32];
        byte[] keyDec = new byte[32];
        Arrays.fill(keyEnc, (byte) 0x11);
        Arrays.fill(keyDec, (byte) 0x22);

        byte[] msg = "secret".getBytes();
        int counter = 42;

        byte[] iv = CryptoProvider.getCcmIv(counter);
        byte[] ct = encryptRef(keyEnc, iv, msg);

        byte[] pt = CryptoProvider.getFromAES256(keyDec, ct, counter);
        assertNull("Decryption with wrong key should return null", pt);
    }

    @Test
    public void wrongCounter_returnsNull()
    {
        byte[] key = new byte[32];
        Arrays.fill(key, (byte) 0x33);

        byte[] msg = "nonce mismatch".getBytes();

        byte[] ivGood = CryptoProvider.getCcmIv(10);
        byte[] ct = encryptRef(key, ivGood, msg);

        // Try decrypt with wrong counter (wrong IV)
        byte[] pt = CryptoProvider.getFromAES256(key, ct, 11);
        assertNull("Wrong counter/IV should return null", pt);
    }

    @Test
    public void tamperedCiphertext_returnsNull()
    {
        byte[] key = new byte[32];
        Arrays.fill(key, (byte) 0x44);

        byte[] msg = "integrity check".getBytes();
        int counter = 99;

        byte[] iv = CryptoProvider.getCcmIv(counter);
        byte[] ct = encryptRef(key, iv, msg);

        // Flip a bit to corrupt
        byte[] tampered = Arrays.copyOf(ct, ct.length);
        tampered[0] ^= 0x01;

        byte[] pt = CryptoProvider.getFromAES256(key, tampered, counter);
        assertNull("Tampered ciphertext should fail authentication and return null", pt);
    }

    @Test
    public void invalidKeyLength_returnsNull()
    {
        byte[] badKey = new byte[31]; // invalid length for AES
        byte[] msg = "abc".getBytes();
        int counter = 1;

        byte[] iv = CryptoProvider.getCcmIv(counter);
        byte[] ct = encryptRef(Arrays.copyOf(badKey, 32), iv, msg);

        byte[] pt = CryptoProvider.getFromAES256(badKey, ct, counter);
        assertNull("Invalid key length should cause exception and return null", pt);
    }

    @Test
    public void nullMessage_returnsNull()
    {
        byte[] key = new byte[32];
        Arrays.fill(key, (byte) 0x66);

        byte[] pt = CryptoProvider.getFromAES256(key, null, 5);
        assertNull("Null ciphertext should be handled and return null", pt);
    }
}
