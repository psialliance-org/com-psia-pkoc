package com.psia.pkoc;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.protobuf.ByteString;
import com.google.protobuf.MessageLite;
import com.psia.pkoc.core.CryptoProvider;
import com.psia.pkoc.databinding.ActivityLoginBinding;
import com.sentryinteractive.opencredential.api.common.CredentialType;
import com.sentryinteractive.opencredential.api.verification.CompleteEmailVerificationRequest;
import com.sentryinteractive.opencredential.api.verification.StartEmailVerificationRequest;
import com.sentryinteractive.opencredential.api.verification.StartEmailVerificationResponse;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity
{
    private static final String TAG = "LoginActivity";
    private static final String PREFS_NAME = "pkoc_login_prefs";
    private static final String KEY_VERIFICATION_COMPLETED = "verification_completed";
    private static final String KEY_VERIFIED_EMAIL = "verified_email";
    private static final String BASE_URL = "https://api.opencredential.sentryinteractive.com";
    private static final String SERVICE_PATH = "/com.sentryinteractive.opencredential.verification.v1alpha.VerificationService";
    private static final MediaType GRPC_WEB_MEDIA_TYPE = MediaType.parse("application/grpc-web");

    private ActivityLoginBinding binding;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private OkHttpClient httpClient;
    private String verificationToken;
    private String currentEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        if (isVerified())
        {
            navigateToMain();
            return;
        }

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        CryptoProvider.initializeCredentials(this);
        httpClient = new OkHttpClient();
        setupListeners();
    }

    private boolean isVerified()
    {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(KEY_VERIFICATION_COMPLETED, false);
    }

    private void setVerified(String email)
    {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit()
                .putBoolean(KEY_VERIFICATION_COMPLETED, true)
                .putString(KEY_VERIFIED_EMAIL, email)
                .apply();
    }

    private void navigateToMain()
    {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void setupListeners()
    {
        binding.sendCodeButton.setOnClickListener(v -> onSendCodeClicked());
        binding.verifyButton.setOnClickListener(v -> onVerifyClicked());
        binding.resendCodeButton.setOnClickListener(v -> onSendCodeClicked());
    }

    // --- gRPC-Web helpers ---

    private byte[] frameGrpcWeb(MessageLite message)
    {
        byte[] msgBytes = message.toByteArray();
        ByteBuffer buf = ByteBuffer.allocate(5 + msgBytes.length);
        buf.put((byte) 0x00);        // no compression
        buf.putInt(msgBytes.length);  // 4-byte big-endian length
        buf.put(msgBytes);
        return buf.array();
    }

    private byte[] parseGrpcWebDataFrame(byte[] responseBytes) throws IOException
    {
        if (responseBytes.length < 5)
        {
            throw new IOException("gRPC-Web response too short: " + responseBytes.length);
        }
        ByteBuffer buf = ByteBuffer.wrap(responseBytes);
        byte flags = buf.get();
        int length = buf.getInt();
        if ((flags & 0x80) != 0)
        {
            throw new IOException("Expected data frame, got trailer frame");
        }
        if (responseBytes.length < 5 + length)
        {
            throw new IOException("gRPC-Web response truncated");
        }
        byte[] msgBytes = new byte[length];
        buf.get(msgBytes);
        return msgBytes;
    }

    private int extractGrpcStatus(Response response, byte[] responseBytes)
    {
        // 1. Check HTTP header
        String header = response.header("grpc-status");
        if (header != null)
        {
            return Integer.parseInt(header);
        }
        // 2. Scan for trailer frame in body (flag byte bit 7 set)
        int offset = 0;
        while (offset + 5 <= responseBytes.length)
        {
            byte flags = responseBytes[offset];
            int len = ByteBuffer.wrap(responseBytes, offset + 1, 4).getInt();
            if ((flags & 0x80) != 0 && offset + 5 + len <= responseBytes.length)
            {
                String trailers = new String(responseBytes, offset + 5, len);
                for (String line : trailers.split("\r\n"))
                {
                    if (line.startsWith("grpc-status:"))
                    {
                        return Integer.parseInt(line.substring("grpc-status:".length()).trim());
                    }
                }
            }
            offset += 5 + len;
        }
        return response.isSuccessful() ? 0 : 2;
    }

    private String extractGrpcMessage(Response response, byte[] responseBytes)
    {
        String header = response.header("grpc-message");
        if (header != null) return header;

        int offset = 0;
        while (offset + 5 <= responseBytes.length)
        {
            byte flags = responseBytes[offset];
            int len = ByteBuffer.wrap(responseBytes, offset + 1, 4).getInt();
            if ((flags & 0x80) != 0 && offset + 5 + len <= responseBytes.length)
            {
                String trailers = new String(responseBytes, offset + 5, len);
                for (String line : trailers.split("\r\n"))
                {
                    if (line.startsWith("grpc-message:"))
                    {
                        return line.substring("grpc-message:".length()).trim();
                    }
                }
            }
            offset += 5 + len;
        }
        return "";
    }

    private byte[] callGrpcWeb(String method, MessageLite requestMessage) throws IOException, GrpcWebException
    {
        byte[] body = frameGrpcWeb(requestMessage);

        Request request = new Request.Builder()
                .url(BASE_URL + SERVICE_PATH + "/" + method)
                .post(RequestBody.create(body, GRPC_WEB_MEDIA_TYPE))
                .addHeader("Accept", "application/grpc-web")
                .addHeader("X-Grpc-Web", "1")
                .build();

        try (Response response = httpClient.newCall(request).execute())
        {
            byte[] responseBytes = response.body() != null ? response.body().bytes() : new byte[0];

            int status = extractGrpcStatus(response, responseBytes);
            if (status != 0)
            {
                throw new GrpcWebException(status, extractGrpcMessage(response, responseBytes));
            }
            if (!response.isSuccessful())
            {
                throw new IOException("HTTP error: " + response.code());
            }
            return responseBytes;
        }
    }

    // --- UI actions ---

    private void onSendCodeClicked()
    {
        String email = binding.emailInput.getText() != null
                ? binding.emailInput.getText().toString().trim()
                : "";

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches())
        {
            showEmailError(getString(R.string.login_error_invalid_email));
            return;
        }

        currentEmail = email;
        setLoading(true);
        hideErrors();

        executor.execute(() ->
        {
            try
            {
                byte[] publicKeyDer = CryptoProvider.GetPublicKey().getEncoded();

                StartEmailVerificationRequest request = StartEmailVerificationRequest.newBuilder()
                        .setEmail(email)
                        .setCredential(ByteString.copyFrom(publicKeyDer))
                        .setCredentialType(CredentialType.CREDENTIAL_TYPE_P256)
                        .setAttestationDocument("TBD")
                        .build();

                byte[] responseBytes = callGrpcWeb("StartEmailVerification", request);
                byte[] msgBytes = parseGrpcWebDataFrame(responseBytes);
                StartEmailVerificationResponse response = StartEmailVerificationResponse.parseFrom(msgBytes);
                verificationToken = response.getVerificationToken();

                Log.d(TAG, "StartEmailVerification success, token: " + verificationToken);

                runOnUiThread(() ->
                {
                    setLoading(false);
                    showCodeStep();
                    showCodeStatus(getString(R.string.login_code_sent));
                });
            }
            catch (NullPointerException e)
            {
                Log.e(TAG, "CryptoProvider.GetPublicKey().getEncoded() threw an error: ", e);
                runOnUiThread(() ->
                {
                    setLoading(false);
                    showEmailError(getString(R.string.login_error_key) + " (" + e + ")");
                });
            }
            catch (GrpcWebException e)
            {
                Log.e(TAG, "gRPC-Web error during StartEmailVerification: " + e.statusCode + " " + e.grpcMessage, e);
                runOnUiThread(() ->
                {
                    setLoading(false);
                    showEmailError(getString(R.string.login_error_network) + " (" + e.statusName() + ")");
                });
            }
            catch (Exception e)
            {
                Log.e(TAG, "Error during StartEmailVerification", e);
                runOnUiThread(() ->
                {
                    setLoading(false);
                    showEmailError(getString(R.string.login_error_network));
                });
            }
        });
    }

    private void onVerifyClicked()
    {
        String code = binding.codeInput.getText() != null
                ? binding.codeInput.getText().toString().trim()
                : "";

        if (TextUtils.isEmpty(code))
        {
            showCodeError(getString(R.string.login_error_empty_code));
            return;
        }

        setLoading(true);
        hideErrors();

        executor.execute(() ->
        {
            try
            {
                CompleteEmailVerificationRequest request = CompleteEmailVerificationRequest.newBuilder()
                        .setToken(verificationToken)
                        .setCode(code)
                        .build();

                callGrpcWeb("CompleteEmailVerification", request);

                Log.d(TAG, "CompleteEmailVerification success");

                runOnUiThread(() ->
                {
                    setLoading(false);
                    setVerified(currentEmail);
                    navigateToMain();
                });
            }
            catch (GrpcWebException e)
            {
                Log.e(TAG, "gRPC-Web error during CompleteEmailVerification: " + e.statusCode + " " + e.grpcMessage, e);
                runOnUiThread(() ->
                {
                    setLoading(false);
                    String message;
                    switch (e.statusCode)
                    {
                        case 3:  // INVALID_ARGUMENT
                        case 5:  // NOT_FOUND
                            message = getString(R.string.login_error_invalid_code);
                            break;
                        case 4:  // DEADLINE_EXCEEDED
                            message = getString(R.string.login_error_expired);
                            break;
                        default:
                            message = getString(R.string.login_error_network) + " (" + e.statusName() + ")";
                            break;
                    }
                    showCodeError(message);
                });
            }
            catch (Exception e)
            {
                Log.e(TAG, "Error during CompleteEmailVerification", e);
                runOnUiThread(() ->
                {
                    setLoading(false);
                    showCodeError(getString(R.string.login_error_network));
                });
            }
        });
    }

    // --- UI helpers ---

    private void showCodeStep()
    {
        binding.emailStep.setVisibility(View.GONE);
        binding.codeStep.setVisibility(View.VISIBLE);
    }

    private void setLoading(boolean loading)
    {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.sendCodeButton.setEnabled(!loading);
        binding.verifyButton.setEnabled(!loading);
        binding.resendCodeButton.setEnabled(!loading);
    }

    private void showEmailError(String message)
    {
        binding.emailError.setText(message);
        binding.emailError.setVisibility(View.VISIBLE);
    }

    private void showCodeError(String message)
    {
        binding.codeError.setText(message);
        binding.codeError.setVisibility(View.VISIBLE);
    }

    private void showCodeStatus(String message)
    {
        binding.codeError.setTextColor(getColor(R.color.colorPrimaryDark));
        binding.codeError.setText(message);
        binding.codeError.setVisibility(View.VISIBLE);
    }

    private void hideErrors()
    {
        binding.emailError.setVisibility(View.GONE);
        binding.codeError.setVisibility(View.GONE);
        binding.codeError.setTextColor(getColor(android.R.color.holo_red_dark));
    }

    // --- gRPC-Web error type ---

    private static class GrpcWebException extends Exception
    {
        final int statusCode;
        final String grpcMessage;

        GrpcWebException(int statusCode, String grpcMessage)
        {
            super("gRPC status " + statusCode + ": " + grpcMessage);
            this.statusCode = statusCode;
            this.grpcMessage = grpcMessage;
        }

        String statusName()
        {
            switch (statusCode)
            {
                case 0: return "OK";
                case 1: return "CANCELLED";
                case 2: return "UNKNOWN";
                case 3: return "INVALID_ARGUMENT";
                case 4: return "DEADLINE_EXCEEDED";
                case 5: return "NOT_FOUND";
                case 6: return "ALREADY_EXISTS";
                case 7: return "PERMISSION_DENIED";
                case 8: return "RESOURCE_EXHAUSTED";
                case 9: return "FAILED_PRECONDITION";
                case 10: return "ABORTED";
                case 11: return "OUT_OF_RANGE";
                case 12: return "UNIMPLEMENTED";
                case 13: return "INTERNAL";
                case 14: return "UNAVAILABLE";
                case 15: return "DATA_LOSS";
                case 16: return "UNAUTHENTICATED";
                default: return "STATUS_" + statusCode;
            }
        }
    }
}
