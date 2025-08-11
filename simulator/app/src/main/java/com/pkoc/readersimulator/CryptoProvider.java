package com.pkoc.readersimulator;

import static java.lang.System.arraycopy;

import android.security.keystore.KeyProperties;
import android.util.Log;

import org.bouncycastle.asn1.x9.ECNamedCurveTable;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.jce.ECPointUtil;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECNamedCurveSpec;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.BigIntegers;
import org.bouncycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class CryptoProvider {
    private static final String TAG = "CryptoProvider";
    final static byte[] IvCounter = Hex.decode("00000001");

    public static byte[] getPublicKeyComponentX(byte[] publicKey) {
        byte[] x = new byte[32];
        arraycopy(publicKey, 27, x, 0, 32);
        return x;
    }

    public static byte[] getUncompressedPublicKeyBytes(byte[] publicKey) {
        byte[] pubKey = new byte[65];
        arraycopy(publicKey, 26, pubKey, 0, 65);
        return pubKey;
    }

    public static byte[] getCompressedPublicKeyBytes(byte[] publicKey) {
        byte[] pubKey = new byte[33];
        arraycopy(publicKey, 26, pubKey, 0, 33);

        byte[] yByteArray = new byte[32];
        arraycopy(publicKey, 59, yByteArray, 0, 32);
        BigInteger y = new BigInteger(yByteArray);

        pubKey[0] = BigIntegers.asUnsignedByteArray(y.mod(BigInteger.valueOf(2)).add(BigInteger.valueOf(2)))[0];

        return pubKey;
    }

    public static Key fromUncompressedPrivateKey(byte[] privateKey) {
        try {
            EncodedKeySpec encodedKeySpec = new PKCS8EncodedKeySpec(privateKey);
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            return keyFactory.generatePrivate(encodedKeySpec);
        } catch (Exception e) {
            Log.e(TAG, "Error generating private key from uncompressed bytes", e);
            return null;
        }
    }

    public static Key fromUncompressedPublicKey(byte[] pubKey) {
        try {
            ECNamedCurveParameterSpec spec = org.bouncycastle.jce.ECNamedCurveTable.getParameterSpec("secp256r1");
            KeyFactory kf = KeyFactory.getInstance("ECDSA", new BouncyCastleProvider());
            ECNamedCurveSpec params = new ECNamedCurveSpec("prime256v1", spec.getCurve(), spec.getG(), spec.getN());
            ECPoint point = ECPointUtil.decodePoint(params.getCurve(), pubKey);
            ECPublicKeySpec pubKeySpec = new ECPublicKeySpec(point, params);
            return kf.generatePublic(pubKeySpec);
        } catch (Exception e) {
            Log.e(TAG, "Error generating public key from uncompressed bytes", e);
            return null;
        }
    }

    public static byte[] getSHA256(byte[] message) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA256");
            return md.digest(message);
        } catch (Exception e) {
            Log.e(TAG, "Error generating SHA256 hash", e);
            return null;
        }
    }

    public static byte[] getFromAES256(byte[] secretKey, byte[] message, int counter) {
        try {
            BigInteger bigIntegerCounter = new BigInteger(IvCounter);
            bigIntegerCounter = bigIntegerCounter.add(BigInteger.valueOf(counter));
            byte[] iv = Arrays.concatenate(Hex.decode("00000000000001"), BigIntegers.asUnsignedByteArray(bigIntegerCounter));

            Log.d(TAG, "Printing the IV: " + Hex.toHexString(iv));

            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey, "AES");
            IvParameterSpec parameterSpec = new IvParameterSpec(iv);
            Cipher cipher = Cipher.getInstance("AES/CCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, parameterSpec);
            return cipher.doFinal(message);
        } catch (Exception e) {
            Log.e(TAG, "Error decrypting AES256 message", e);
            return null;
        }
    }

    public static ECDomainParameters getDomainParameters() {
        X9ECParameters domain = ECNamedCurveTable.getByName("secp256r1");
        return new ECDomainParameters(domain.getCurve(), domain.getG(), domain.getN(), domain.getH(), domain.getSeed());
    }

    public static byte[] GetSignedMessage(byte[] data) {
        try {
            Signature s = Signature.getInstance("SHA256withECDSA");
            s.initSign((PrivateKey) fromUncompressedPrivateKey(Hex.decode(ReaderProfile.SitePrivateKey)));
            s.update(data);
            return s.sign();
        } catch (Exception e) {
            Log.e(TAG, "Error signing message", e);
            return null;
        }
    }

    public static KeyPair CreateTransientKeyPair() {
        try {
            KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC);
            keyGenerator.initialize(256);
            return keyGenerator.generateKeyPair();
        } catch (Exception e) {
            Log.e(TAG, "Error creating transient key pair", e);
            return null;
        }
    }

    public static byte[] getSharedSecret(Key privateKey, byte[] publicKey) {
        try {
            KeyAgreement keyAgreement = KeyAgreement.getInstance("ECDH");
            keyAgreement.init(privateKey);
            keyAgreement.doPhase(fromUncompressedPublicKey(publicKey), true);
            return keyAgreement.generateSecret();
        } catch (Exception e) {
            Log.e(TAG, "Error generating shared secret", e);
            return null;
        }
    }
}
