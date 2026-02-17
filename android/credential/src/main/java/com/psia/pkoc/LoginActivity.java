package com.psia.pkoc;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.psia.pkoc.core.CryptoProvider;
import com.psia.pkoc.core.grpc.CredentialService;
import com.psia.pkoc.databinding.ActivityLoginBinding;
import com.psia.pkoc.core.grpc.GrpcWebException;
import com.psia.pkoc.core.grpc.VerificationService;
import com.sentryinteractive.opencredential.api.common.CredentialType;
import com.sentryinteractive.opencredential.api.common.Identity;
import com.sentryinteractive.opencredential.api.credential.Credential;
import com.sentryinteractive.opencredential.api.credential.CredentialFilter;
import com.sentryinteractive.opencredential.api.credential.GetCredentialsResponse;
import com.sentryinteractive.opencredential.api.verification.StartEmailVerificationResponse;

import org.bouncycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity
{
    private static final String TAG = "LoginActivity";

    /** When true, finish() with RESULT_OK after 2FA instead of navigating. */
    public static final String EXTRA_RETURN_ON_SUCCESS = "return_on_success";

    private ActivityLoginBinding binding;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private boolean returnOnSuccess;
    private String verificationToken;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        returnOnSuccess = getIntent().getBooleanExtra(EXTRA_RETURN_ON_SUCCESS, false);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        CryptoProvider.initializeCredentials(this);

        if (!returnOnSuccess)
        {
            checkSavedCredentials();
        }
        else
        {
            setupListeners();
        }
    }

    private void checkSavedCredentials()
    {
        Set<String> savedIds = CredentialStore.getSelectedCredentialIds(this);
        if (savedIds.isEmpty())
        {
            showNoContext();
            return;
        }

        // Show loading while we verify credentials against server
        binding.emailStep.setVisibility(View.GONE);
        binding.progressBar.setVisibility(View.VISIBLE);

        executor.execute(() ->
        {
            try
            {
                GetCredentialsResponse response = CredentialService.getInstance()
                        .getCredentials(CredentialFilter.CREDENTIAL_FILTER_SAME_KEY);

                List<Credential> matched = new ArrayList<>();
                for (Credential cred : response.getCredentialsList())
                {
                    if (!cred.hasIdentity()
                            || cred.getIdentity().getIdentityCase() != Identity.IdentityCase.EMAIL)
                    {
                        continue;
                    }
                    String hexId = Hex.toHexString(cred.getCredential().toByteArray());
                    if (savedIds.contains(hexId))
                    {
                        matched.add(cred);
                    }
                }

                runOnUiThread(() ->
                {
                    binding.progressBar.setVisibility(View.GONE);
                    if (matched.isEmpty())
                    {
                        CredentialStore.clear(LoginActivity.this);
                        showNoContext();
                    }
                    else
                    {
                        navigateToMainWithCredentials(matched);
                    }
                });
            }
            catch (Exception e)
            {
                Log.e(TAG, "Failed to verify saved credentials", e);
                runOnUiThread(() ->
                {
                    binding.progressBar.setVisibility(View.GONE);
                    showNoContext();
                });
            }
        });
    }

    private void navigateToMainWithCredentials(List<Credential> credentials)
    {
        Intent intent = new Intent(this, MainActivity.class);
        int count = 0;
        for (Credential cred : credentials)
        {
            intent.putExtra(CredentialSelectionActivity.EXTRA_CREDENTIAL_PREFIX + count, cred.toByteArray());
            count++;
        }
        intent.putExtra(CredentialSelectionActivity.EXTRA_SELECTED_COUNT, count);
        startActivity(intent);
        finish();
    }

    private void showNoContext()
    {
        binding.emailStep.setVisibility(View.GONE);
        binding.codeStep.setVisibility(View.GONE);
        binding.title.setVisibility(View.GONE);
        binding.noContextContainer.setVisibility(View.VISIBLE);
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

        setLoading(true);
        hideErrors();

        executor.execute(() ->
        {
            try
            {
                byte[] publicKeyDer = Objects.requireNonNull(CryptoProvider.GetPublicKey()).getEncoded();

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
                    if (returnOnSuccess)
                    {
                        setResult(RESULT_OK);
                        finish();
                    }
                    else
                    {
                        navigateToCredentialSelection();
                    }
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