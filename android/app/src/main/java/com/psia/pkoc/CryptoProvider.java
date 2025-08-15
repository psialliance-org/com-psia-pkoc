package com.psia.pkoc;

import static java.lang.System.arraycopy;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;

import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x9.ECNamedCurveTable;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.modes.CCMBlockCipher;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.jce.ECPointUtil;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECNamedCurveSpec;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.BigIntegers;
import org.bouncycastle.util.encoders.Hex;
import org.bouncycastle.crypto.modes.CCMModeCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.engines.AESEngine;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECPublicKeySpec;
import java.util.Objects;

import javax.crypto.KeyAgreement;

/**
 * Helper class for providing cryptographic functionality
 */
public class CryptoProvider
{
    final static String AndroidKeyStore = "AndroidKeyStore";
    /** @noinspection SpellCheckingInspection*/
    final static String KeyAlgorithm = "ECDSA";
    /** @noinspection SpellCheckingInspection*/
    final static String HandshakeAlgorithm = "ECDH";
    final static String KeyAndHashAlgorithm = "SHA256withECDSA";
    /** @noinspection SpellCheckingInspection*/
    final static String NamedCurve = "secp256r1";
    final static String HashAlgorithm = "SHA256";
    final static byte[] IvPrepend = new byte[] { 0, 0, 0, 0, 0, 0, 0, 1 };
    final static int CcmTagLength = 128;
    private static KeyGenParameterSpec.Builder getKeyGenParamaterSpecBuilder()
    {
        ECGenParameterSpec ecParam = new ECGenParameterSpec(NamedCurve);

        return new KeyGenParameterSpec.Builder(PKOC_Preferences.PKOC_CredentialSet,
            KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
            .setUserAuthenticationRequired(false)
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setRandomizedEncryptionRequired(false)
            .setKeySize(256)
            .setAlgorithmParameterSpec(ecParam);
    }

    /**
     * Create key pair
     */
    public static void CreateKeyPair ()
    {
        try
        {
            KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, AndroidKeyStore);
            var builder = getKeyGenParamaterSpecBuilder();
            keyGenerator.initialize(builder.build());
            keyGenerator.generateKeyPair();
        }
        catch (Exception ignored)
        {
            // The provided hard-coded values will not throw an exception
        }
    }

    /**
     * Create a transient key pair
     * @return KeyPair
     */
    public static KeyPair CreateTransientKeyPair ()
    {
        try
        {
            KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC);
            keyGenerator.initialize(256);
            return keyGenerator.generateKeyPair();
        }
        catch (Exception ignored)
        {
            throw new RuntimeException();
        }
    }

    /**
     * Get device's public key
     * @return Device's public key
     */
    public static Key GetPublicKey ()
    {
        KeyStore ks;
        Key pk;

        try
        {
            ks = KeyStore.getInstance(AndroidKeyStore);
            ks.load(null);
            Certificate entry = ks.getCertificate(PKOC_Preferences.PKOC_CredentialSet);
            pk = entry.getPublicKey();
        }
        catch (Exception e)
        {
            return null;
        }

        return pk;
    }

    /**
     * Get uncompressed public key x bytes
     * @param publicKey DER-encoded public key
     * @return Public key as x
     */
    public static byte[] getPublicKeyComponentX(byte[] publicKey)
    {
        byte[] x = new byte[32];
        arraycopy(publicKey, 27, x, 0, 32);
        return x;
    }

    /**
     * Get uncompressed public key bytes
     * @param publicKey DER-encoded public key
     * @return Public key as 0x01 | x | y
     */
    public static byte[] getUncompressedPublicKeyBytes (byte[] publicKey)
    {
        byte[] pubKey = new byte[65];
        arraycopy(publicKey, 26, pubKey, 0, 65);
        return pubKey;
    }

    /**
     * Get uncompressed public key bytes
     * @return Public key as 0x01 | x | y
     */
    public static byte[] getUncompressedPublicKeyBytes ()
    {
        return getUncompressedPublicKeyBytes(Objects.requireNonNull(GetPublicKey()).getEncoded());
    }

    /**
     * Get SHA256 hash of a message
     * @param message message
     * @return hash
     */
    public static byte[] getSHA256(byte[] message)
    {
        try
        {
            MessageDigest md = MessageDigest.getInstance(HashAlgorithm);
            return md.digest(message);
        }
        catch (Exception ignored)
        {
            throw new RuntimeException();
        }
    }

    public static byte[] getCcmIv(int counter)
    {
        ByteBuffer buf = ByteBuffer.allocate(12).order(ByteOrder.BIG_ENDIAN);
        buf.put(IvPrepend);
        buf.putInt(counter);
        return buf.array();
    }

    /**
     * Get AES256 encrypted data
     * @param secretKey Secret key for AES encryption
     * @param message Message to encrypt
     * @return Encrypted message
     */

    public static byte[] getAES256(byte[] secretKey, byte[] message, int counter){
        try {
            byte[] iv = getCcmIv(counter);
            Log.d("CryptoProvider", "Printing the secret key " + Hex.toHexString(secretKey));
            Log.d("CryptoProvider", "Printing the IV " + Hex.toHexString(iv));

            CCMModeCipher ccm = CCMBlockCipher.newInstance(AESEngine.newInstance());
            AEADParameters params = new AEADParameters(new KeyParameter(secretKey), CcmTagLength, iv); // 128-bit tag
            ccm.init(true, params); // true = encryption

            byte[] output = new byte[ccm.getOutputSize(message.length)];
            int len = ccm.processBytes(message, 0, message.length, output, 0);
            ccm.doFinal(output, len);

            Log.d("CryptoProvider", "Encrypted data length: " + output.length);
            Log.d("CryptoProvider", "Encrypted data (hex): " + Hex.toHexString(output));

            return output;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Get domain parameters
     * @return ECDomainParameters for NIST P-256
     */
    public static ECDomainParameters getDomainParameters()
    {
        X9ECParameters domain = ECNamedCurveTable.getByName(NamedCurve);
        return new ECDomainParameters(domain.getCurve(), domain.getG(), domain.getN(), domain.getH(), domain.getSeed());
    }

    /**
     * From compressed public key
     * @param pubKey Get a key from a 33 byte long byte array
     * @return Public key
     */
    public static Key fromCompressedPublicKey(byte[] pubKey)
    {
        try
        {
            ECNamedCurveParameterSpec spec = org.bouncycastle.jce.ECNamedCurveTable.getParameterSpec(NamedCurve);
            KeyFactory kf = KeyFactory.getInstance(KeyAlgorithm, new BouncyCastleProvider());
            ECNamedCurveSpec params = new ECNamedCurveSpec(NamedCurve, spec.getCurve(), spec.getG(), spec.getN());
            java.security.spec.ECPoint point = ECPointUtil.decodePoint(params.getCurve(), pubKey);
            ECPublicKeySpec pubKeySpec = new ECPublicKeySpec(point, params);
            return kf.generatePublic(pubKeySpec);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Validate a signed message
     * @param publicKey Public key of signer
     * @param message Original message
     * @param signature Message signature
     * @return true/false
     */
    public static boolean validateSignedMessage(byte[] publicKey, byte[] message, byte[] signature)
    {
        if (publicKey.length < 65 || signature.length < 64)
        {
            return false;
        }

        byte[] x = new byte[32], y = new byte[32], r = new byte[32], s = new byte[32];
        arraycopy(publicKey, 1, x, 0, 32);
        arraycopy(publicKey, 33, y, 0, 32);
        arraycopy(signature, 0, r, 0, 32);
        arraycopy(signature, 32, s, 0, 32);

        boolean sigValid = false;

        BigInteger xi = new BigInteger(1, x);
        BigInteger yi = new BigInteger(1, y);
        BigInteger ri = new BigInteger(1, r);
        BigInteger si = new BigInteger(1, s);

        try
        {
            ECDomainParameters ecParams = getDomainParameters();

            ECPoint ecPoint = ecParams.getCurve().createPoint(xi, yi);
            ECPublicKeyParameters pubKeyParams = new ECPublicKeyParameters(ecPoint, ecParams);

            ECDSASigner ecSign = new ECDSASigner();
            ecSign.init(false, pubKeyParams);

            final byte[] hash = getSHA256(message);
            sigValid = ecSign.verifySignature(hash, ri, si);
        }
        catch (Exception e)
        {
            Log.d("MainActivity", e.toString());
        }

        return sigValid;
    }

    /**
     * Remove ASN header from signature
     * @param signature ASN1/DER encoded signature
     * @return 64 byte byte arraying containing r|s
     */
    public static byte[] RemoveASNHeaderFromSignature(byte[] signature)
    {
        ASN1Sequence seq = ASN1Sequence.getInstance(signature);
        byte[] r = BigIntegers.asUnsignedByteArray(ASN1Integer.getInstance(seq.getObjectAt(0)).getPositiveValue());
        byte[] s = BigIntegers.asUnsignedByteArray(ASN1Integer.getInstance(seq.getObjectAt(1)).getPositiveValue());

        byte[] r32 = new byte[32], s32 = new byte[32];
        arraycopy(r, 0, r32, 32 - r.length, r.length);
        arraycopy(s, 0, s32, 32 - s.length, s.length);

        return Arrays.concatenate(r32, s32);
    }

    /**
     * Sign a message with device's public key
     * @param data Data to sign
     * @return Signing data
     */
    public static byte[] GetSignedMessage (byte[] data)
    {
        try
        {
            KeyStore ks = KeyStore.getInstance(AndroidKeyStore);
            ks.load(null);
            KeyStore.Entry entry = ks.getEntry(PKOC_Preferences.PKOC_CredentialSet, null);

            Signature s = Signature.getInstance(KeyAndHashAlgorithm);
            s.initSign(((KeyStore.PrivateKeyEntry) entry).getPrivateKey());
            s.update(data);

            return s.sign();
        }
        catch (Exception e)
        {
            return new byte[64];
        }
    }

    /**
     * Get a shared secret
     * @param privateKey Our private key
     * @param publicKey Their public key
     * @return Shared secret
     */
    public static byte[] getSharedSecret(PrivateKey privateKey, byte[] publicKey)
    {
        try
        {
            KeyAgreement keyAgreement = KeyAgreement.getInstance(HandshakeAlgorithm);
            keyAgreement.init(privateKey);
            keyAgreement.doPhase(fromCompressedPublicKey(publicKey), true);
            return keyAgreement.generateSecret();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public static byte[] getFromAES256(byte[] secretKey, byte[] message, int counter) {
        try {
            byte[] iv = getCcmIv(counter);
            CCMModeCipher ccm = CCMBlockCipher.newInstance(AESEngine.newInstance());
            AEADParameters params = new AEADParameters(new KeyParameter(secretKey), 128, iv);
            ccm.init(false, params); // false = decryption

            byte[] output = new byte[ccm.getOutputSize(message.length)];
            int len = ccm.processBytes(message, 0, message.length, output, 0);
            ccm.doFinal(output, len);

            return output;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
