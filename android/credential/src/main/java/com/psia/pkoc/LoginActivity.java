package com.psia.pkoc;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.psia.pkoc.core.CryptoProvider;
import com.psia.pkoc.databinding.ActivityLoginBinding;
import com.psia.pkoc.core.grpc.GrpcWebException;
import com.psia.pkoc.core.grpc.VerificationService;
import com.sentryinteractive.opencredential.api.common.CredentialType;
import com.sentryinteractive.opencredential.api.verification.StartEmailVerificationResponse;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity
{
    private static final String TAG = "LoginActivity";
    private static final String PREFS_NAME = "pkoc_login_prefs";
    private static final String KEY_VERIFICATION_COMPLETED = "verification_completed";
    private static final String KEY_VERIFIED_EMAIL = "verified_email";

    private ActivityLoginBinding binding;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private String verificationToken;
    private String currentEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // TODO: re-enable auto-redirect to PKOC template when flow is finalised
        // if (isVerified())
        // {
        //     navigateToMain();
        //     return;
        // }

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        CryptoProvider.initializeCredentials(this);
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

    private void navigateToCredentialSelection()
    {
        Intent intent = new Intent(this, CredentialSelectionActivity.class);
        startActivity(intent);
        finish();
    }

    private void setupListeners()
    {
        binding.sendCodeButton.setOnClickListener(v -> onSendCodeClicked());
        binding.verifyButton.setOnClickListener(v -> onVerifyClicked());
        binding.resendCodeButton.setOnClickListener(v -> onSendCodeClicked());
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

                StartEmailVerificationResponse response = VerificationService.getInstance()
                        .startEmailVerification(
                                email,
                                publicKeyDer,
                                CredentialType.CREDENTIAL_TYPE_P256,
                                "TBD");

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
                Log.e(TAG, "gRPC-Web error during StartEmailVerification: " + e.getStatusCode() + " " + e.getGrpcMessage(), e);
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
                VerificationService.getInstance()
                        .completeEmailVerification(verificationToken, code);

                Log.d(TAG, "CompleteEmailVerification success");

                runOnUiThread(() ->
                {
                    setLoading(false);
                    setVerified(currentEmail);
                    navigateToCredentialSelection();
                });
            }
            catch (GrpcWebException e)
            {
                Log.e(TAG, "gRPC-Web error during CompleteEmailVerification: " + e.getStatusCode() + " " + e.getGrpcMessage(), e);
                runOnUiThread(() ->
                {
                    setLoading(false);
                    String message;
                    switch (e.getStatusCode())
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
}