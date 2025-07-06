package com.psia.pkoc;

import static java.lang.System.arraycopy;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;

import org.bouncycastle.asn1.x9.ECNamedCurveTable;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.jce.ECPointUtil;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECNamedCurveSpec;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.BigIntegers;
import org.bouncycastle.util.encoders.Hex;
import org.bouncycastle.util.Arrays;

import java.math.BigInteger;
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

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Helper class for providing cryptographic functionality
 */
public class CryptoProvider
{
    final static byte[] IvCounter = Hex.decode("AABBCCDD");


    private static KeyGenParameterSpec.Builder getKeyGenParamaterSpecBuilder()
    {
        ECGenParameterSpec ecParam = new ECGenParameterSpec("sec256r1");

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
            KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore");
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
            ks = KeyStore.getInstance("AndroidKeyStore");
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
            MessageDigest md = MessageDigest.getInstance("SHA256");
            return md.digest(message);
        }
        catch (Exception ignored)
        {
            throw new RuntimeException();
        }
    }

    /**
     * Get AES256 encrypted data
     * @param secretKey Secret key for AES encryption
     * @param message Message to encrypt
     * @return Encrypted message
     */
    public static byte[] getAES256(byte[] secretKey, byte[] message, int counter)
    {
        try
        {
            // 1. Convert IvCounter into a BigInteger (assuming IvCounter is a String).
            BigInteger bigIntegerCounter = new BigInteger(IvCounter);
            Log.d("CryptoProvider", "Printing the bigIntegerIV in hex: " + bigIntegerCounter.toString(16));

            // 2. Add the 'counter' value to the BigInteger
            bigIntegerCounter = bigIntegerCounter.add(BigInteger.valueOf(counter));

            // 3. Construct IV by concatenating "00000000000001" (hex-decoded) + the BigInteger bytes
            byte[] iv = Arrays.concatenate(Hex.decode("00000000000001"),
                    BigIntegers.asUnsignedByteArray(bigIntegerCounter));

            Log.d("CryptoProvider", "Printing the secret key " + Hex.toHexString(secretKey));

            Log.d("CryptoProvider", "Printing the IV " + Hex.toHexString(iv));

            // 4. Initialize Cipher
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey, "AES");
            IvParameterSpec parameterSpec = new IvParameterSpec(iv);
            Cipher cipher = Cipher.getInstance("AES/CCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, parameterSpec);

            // 5. Encrypt message
            byte[] encryptedData = cipher.doFinal(message);

            // 6. Log size and hex output of the encrypted data
            Log.d("CryptoProvider", "Encrypted data length: " + encryptedData.length);
            Log.d("CryptoProvider", "Encrypted data (hex): " + Hex.toHexString(encryptedData));

            return encryptedData;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get domain parameters
     * @return ECDomainParameters for secp256r1
     */
    public static ECDomainParameters getDomainParameters()
    {
        X9ECParameters domain = ECNamedCurveTable.getByName("sec256r1");
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
            ECNamedCurveParameterSpec spec = org.bouncycastle.jce.ECNamedCurveTable.getParameterSpec("secp256r1");
            KeyFactory kf = KeyFactory.getInstance("ECDSA", new BouncyCastleProvider());
            ECNamedCurveSpec params = new ECNamedCurveSpec("prime256v1", spec.getCurve(), spec.getG(), spec.getN());
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
     * Sign a message with device's public key
     * @param data Data to sign
     * @return Signing data
     */
    public static byte[] GetSignedMessage (byte[] data)
    {
        try
        {
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            KeyStore.Entry entry = ks.getEntry(PKOC_Preferences.PKOC_CredentialSet, null);

            Signature s = Signature.getInstance("SHA256withECDSA");
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
            KeyAgreement keyAgreement = KeyAgreement.getInstance("ECDH");
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
            BigInteger bigIntegerCounter = new BigInteger(IvCounter);
            bigIntegerCounter = bigIntegerCounter.add(BigInteger.valueOf(counter));
            byte[] iv = Arrays.concatenate(Hex.decode("00000000000001"), BigIntegers.asUnsignedByteArray(bigIntegerCounter));

            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey, "AES");
            IvParameterSpec parameterSpec = new IvParameterSpec(iv);
            Cipher cipher = Cipher.getInstance("AES/CCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, parameterSpec);
            return cipher.doFinal(message);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
