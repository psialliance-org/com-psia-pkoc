package com.psia.pkoc.core.grpc;

import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;

import com.psia.pkoc.core.CryptoProvider;

import java.io.IOException;
import java.security.Key;
import java.security.MessageDigest;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;

/**
 * OkHttp interceptor that adds RFC 9421 HTTP Message Signatures to gRPC-Web requests.
 * Signs with the device's P-256 key from the Android KeyStore when available.
 */
public class GrpcWebInterceptor implements Interceptor
{
    private static final String TAG = "GrpcWebInterceptor";

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException
    {
        Request original = chain.request();

        try
        {
            Key publicKey = CryptoProvider.GetPublicKey();
            if (publicKey == null)
            {
                return chain.proceed(original);
            }

            byte[] bodyBytes = bodyToBytes(original.body());

            // content-digest: sha-256=:<base64>:
            byte[] bodySha256 = MessageDigest.getInstance("SHA-256").digest(bodyBytes);
            String contentDigest = "sha-256=:" + base64(bodySha256) + ":";

            // keyid = base64url-unpadded SHA-256 of the public key DER
            byte[] publicKeyDer = publicKey.getEncoded();
            byte[] thumbprintBytes = MessageDigest.getInstance("SHA-256").digest(publicKeyDer);
            String keyId = base64Url(thumbprintBytes);

            long created = System.currentTimeMillis() / 1000;
            String path = original.url().encodedPath();
            String authority = original.url().host();

            // signature-input value
            String sigInputValue = "(\"@method\" \"@path\" \"@authority\" \"content-digest\")"
                    + ";alg=\"ecdsa-p256-sha256\""
                    + ";keyid=\"" + keyId + "\""
                    + ";created=" + created;

            // signature base per RFC 9421 §2.5
            String sigBase = "\"@method\": POST\n"
                    + "\"@path\": " + path + "\n"
                    + "\"@authority\": " + authority + "\n"
                    + "\"content-digest\": " + contentDigest + "\n"
                    + "\"@signature-params\": " + sigInputValue;

            // sign with device key (SHA256withECDSA → DER-encoded)
            byte[] signature = CryptoProvider.GetSignedMessage(sigBase.getBytes());
            if (signature == null)
            {
                return chain.proceed(original);
            }

            Request signed = original.newBuilder()
                    .header("content-digest", contentDigest)
                    .header("signature-input", "sig1=" + sigInputValue)
                    .header("signature", "sig1=:" + base64(signature) + ":")
                    .build();

            return chain.proceed(signed);
        }
        catch (Exception e)
        {
            Log.w(TAG, "Failed to sign request, proceeding unsigned", e);
            return chain.proceed(original);
        }
    }

    private static byte[] bodyToBytes(RequestBody body) throws IOException
    {
        if (body == null) return new byte[0];
        Buffer buffer = new Buffer();
        body.writeTo(buffer);
        return buffer.readByteArray();
    }

    private static String base64(byte[] data)
    {
        return Base64.encodeToString(data, Base64.NO_WRAP);
    }

    private static String base64Url(byte[] data)
    {
        return Base64.encodeToString(data, Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
    }
}
