package com.pkoc.readersimulator;

import static java.lang.System.arraycopy;

import android.security.keystore.KeyProperties;
import android.util.Log;

import org.bouncycastle.asn1.x9.ECNamedCurveTable;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.CCMBlockCipher;
import org.bouncycastle.crypto.modes.CCMModeCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.jce.ECPointUtil;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECNamedCurveSpec;
import org.bouncycastle.util.BigIntegers;
import org.bouncycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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

import javax.crypto.KeyAgreement;

public class CryptoProvider {
    private static final String TAG = "CryptoProvider";
    final static byte[] IvPrepend = new byte[] { 0, 0, 0, 0, 0, 0, 0, 1 };
    final static int CcmTagLength = 128;

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

    public static byte[] getCcmIv(int counter)
    {
        ByteBuffer buf = ByteBuffer.allocate(12).order(ByteOrder.BIG_ENDIAN);
        buf.put(IvPrepend);
        buf.putInt(counter);
        return buf.array();
    }
    public static byte[] getFromAES256(byte[] secretKey, byte[] message, int counter) {
        try {
            byte[] iv = getCcmIv(counter);
            Log.d(TAG, "Printing the IV: " + Hex.toHexString(iv));

            CCMModeCipher ccm = CCMBlockCipher.newInstance(AESEngine.newInstance());
            AEADParameters params = new AEADParameters(new KeyParameter(secretKey), CcmTagLength, iv); // 128-bit tag
            ccm.init(false, params); // false = decryption

            byte[] output = new byte[ccm.getOutputSize(message.length)];
            int len = ccm.processBytes(message, 0, message.length, output, 0);
            ccm.doFinal(output, len);

            return output;
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

    public static byte[] deriveAesKeyFromSharedSecretSimple(byte[] sharedSecret) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            return sha256.digest(sharedSecret);
        } catch (Exception e) {
            Log.e(TAG, "Error hashing shared secret for AES key derivation", e);
            return null;
        }
    }
}
