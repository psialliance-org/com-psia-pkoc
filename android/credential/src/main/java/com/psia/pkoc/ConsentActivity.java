package com.psia.pkoc;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.psia.pkoc.core.CryptoProvider;
import com.psia.pkoc.core.grpc.OrganizationService;
import com.psia.pkoc.databinding.ActivityConsentBinding;
import com.sentryinteractive.opencredential.api.organization.Organization;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConsentActivity extends AppCompatActivity
{
    private static final String TAG = "ConsentActivity";

    public static final String EXTRA_ORG_ID = "org_id";
    public static final String EXTRA_ORG_NAME = "org_name";
    public static final String EXTRA_INVITE_CODE = "invite_code";

    private ActivityConsentBinding binding;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private String inviteCode;
    private String organizationId;
    private String organizationName;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        binding = ActivityConsentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        CryptoProvider.initializeCredentials(this);

        inviteCode = extractInviteCode();
        if (inviteCode == null)
        {
            Log.e(TAG, "No invite code found in intent");
            finish();
            return;
        }

        setupListeners();
        loadOrganization();
    }

    private String extractInviteCode()
    {
        Uri data = getIntent().getData();
        if (data != null)
        {
            List<String> segments = data.getPathSegments();
            // Expected path: /share/{inviteCode}
            if (segments.size() >= 2 && "share".equals(segments.get(0)))
            {
                return segments.get(1);
            }
        }
        // Fallback: check extras (for testing without deeplink)
        return getIntent().getStringExtra(EXTRA_INVITE_CODE);
    }

    private void setupListeners()
    {
        binding.proceedButton.setOnClickListener(v -> onProceedClicked());
        binding.cancelButton.setOnClickListener(v -> finishAffinity());
        binding.retryButton.setOnClickListener(v -> loadOrganization());
    }

    private void loadOrganization()
    {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.scrollView.setVisibility(View.GONE);
        binding.errorContainer.setVisibility(View.GONE);

        executor.execute(() ->
        {
            try
            {
                Organization org = OrganizationService.getInstance()
                        .getOrganizationByInviteCode(inviteCode);

                organizationId = org.getOrganizationId();
                organizationName = org.getName();

                runOnUiThread(() ->
                {
                    binding.progressBar.setVisibility(View.GONE);
                    populateOrganization(org);
                    binding.scrollView.setVisibility(View.VISIBLE);
                });
            }
            catch (Exception e)
            {
                Log.e(TAG, "Failed to load organization", e);
                runOnUiThread(() ->
                {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.errorText.setText(getString(R.string.consent_error_loading));
                    binding.errorContainer.setVisibility(View.VISIBLE);
                });
            }
        });
    }

    private void populateOrganization(Organization org)
    {
        binding.permissionTitle.setText(
                getString(R.string.consent_permission_title, org.getName()));

        binding.description.setText(
                getString(R.string.consent_description, org.getName()));

        binding.orgName.setText(org.getName());
        binding.orgEmail.setText(org.getContactEmail());

        if (!org.getContactAddress().isEmpty())
        {
            binding.orgAddress.setText(org.getContactAddress());
            binding.orgAddress.setVisibility(View.VISIBLE);
        }

        if (org.hasContactPhone() && !org.getContactPhone().isEmpty())
        {
            binding.orgPhone.setText(org.getContactPhone());
            binding.orgPhone.setVisibility(View.VISIBLE);
        }
    }

    private void onProceedClicked()
    {
        Intent intent = new Intent(this, CredentialSelectionActivity.class);
        intent.putExtra(EXTRA_ORG_ID, organizationId);
        intent.putExtra(EXTRA_ORG_NAME, organizationName);
        intent.putExtra(EXTRA_INVITE_CODE, inviteCode);
        startActivity(intent);
        finish();
    }
}
