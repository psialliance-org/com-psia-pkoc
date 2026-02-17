package com.psia.pkoc;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.psia.pkoc.core.CryptoProvider;
import com.psia.pkoc.core.grpc.CredentialService;
import com.psia.pkoc.databinding.ActivityCredentialSelectionBinding;
import com.sentryinteractive.opencredential.api.common.Identity;
import com.sentryinteractive.opencredential.api.credential.Credential;
import com.sentryinteractive.opencredential.api.credential.CredentialFilter;
import com.sentryinteractive.opencredential.api.credential.GetCredentialsResponse;

import org.bouncycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CredentialSelectionActivity extends AppCompatActivity
{
    private static final String TAG = "CredentialSelection";

    public static final String EXTRA_SELECTED_COUNT = "selected_count";
    public static final String EXTRA_CREDENTIAL_PREFIX = "credential_";

    private ActivityCredentialSelectionBinding binding;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<Credential> emailCredentials = new ArrayList<>();
    private final Map<Integer, CheckBox> checkBoxMap = new HashMap<>();

    private final ActivityResultLauncher<Intent> loginLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result ->
            {
                if (result.getResultCode() == RESULT_OK)
                {
                    loadCredentials();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        binding = ActivityCredentialSelectionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        CryptoProvider.initializeCredentials(this);

        String orgName = getIntent().getStringExtra(ConsentActivity.EXTRA_ORG_NAME);

        if (orgName != null)
        {
            binding.permissionTitle.setText(getString(R.string.cred_sel_permission_title, orgName));
        }

        setupListeners();
        loadCredentials();
    }

    private void setupListeners()
    {
        binding.approveButton.setOnClickListener(v -> onApproveClicked());

        binding.cancelButton.setOnClickListener(v ->
        {
            // TODO: handle cancel
        });

        binding.addNewEmail.setOnClickListener(v ->
        {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.putExtra(LoginActivity.EXTRA_RETURN_ON_SUCCESS, true);
            loginLauncher.launch(intent);
        });

        binding.retryButton.setOnClickListener(v -> loadCredentials());
    }

    private void loadCredentials()
    {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.scrollView.setVisibility(View.GONE);
        binding.errorContainer.setVisibility(View.GONE);

        executor.execute(() ->
        {
            try
            {
                GetCredentialsResponse response = CredentialService.getInstance()
                        .getCredentials(CredentialFilter.CREDENTIAL_FILTER_SAME_KEY);

                List<Credential> filtered = new ArrayList<>();
                for (Credential cred : response.getCredentialsList())
                {
                    if (cred.hasIdentity()
                            && cred.getIdentity().getIdentityCase() == Identity.IdentityCase.EMAIL)
                    {
                        filtered.add(cred);
                    }
                }

                runOnUiThread(() ->
                {
                    binding.progressBar.setVisibility(View.GONE);
                    emailCredentials.clear();
                    emailCredentials.addAll(filtered);
                    populateCredentials();
                    binding.scrollView.setVisibility(View.VISIBLE);
                });
            }
            catch (Exception e)
            {
                Log.e(TAG, "Failed to load credentials", e);
                runOnUiThread(() ->
                {
                    binding.progressBar.setVisibility(View.GONE);
                    // Show empty state — user can add a new email
                    emailCredentials.clear();
                    populateCredentials();
                    binding.scrollView.setVisibility(View.VISIBLE);
                });
            }
        });
    }

    private void populateCredentials()
    {
        binding.credentialList.removeAllViews();
        checkBoxMap.clear();

        binding.approveButton.setEnabled(!emailCredentials.isEmpty());

        if (emailCredentials.isEmpty())
        {
            // Nothing to show — "Add new email" is still visible
            return;
        }

        // Count emails to detect duplicates
        Map<String, Integer> emailCount = new HashMap<>();
        for (Credential cred : emailCredentials)
        {
            String email = cred.getIdentity().getEmail();
            //noinspection DataFlowIssue
            emailCount.put(email, emailCount.getOrDefault(email, 0) + 1);
        }

        // Top divider
        addDivider();

        for (int i = 0; i < emailCredentials.size(); i++)
        {
            Credential cred = emailCredentials.get(i);
            String email = cred.getIdentity().getEmail();
            //noinspection DataFlowIssue
            boolean hasDuplicate = emailCount.containsKey(email) && emailCount.getOrDefault(email, 0) > 1;

            String label = email;
            if (hasDuplicate)
            {
                String keyString = Hex.toHexString(cred.getCredential().toByteArray());
                String ID = "…" + keyString.substring(keyString.length() - 6);
                label += "  (" + ID + ")";
            }

            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(label);
            checkBox.setChecked(true);
            checkBox.setTextColor(Color.parseColor("#212121"));
            checkBox.setTextSize(15);
            checkBox.setPadding(8, 0, 8, 0);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.topMargin = 16;
            params.bottomMargin = 16;
            checkBox.setLayoutParams(params);

            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> updateApproveButton());

            binding.credentialList.addView(checkBox);
            checkBoxMap.put(i, checkBox);

            addDivider();
        }
    }

    private void updateApproveButton()
    {
        boolean anyChecked = false;
        for (CheckBox cb : checkBoxMap.values())
        {
            if (cb.isChecked())
            {
                anyChecked = true;
                break;
            }
        }
        binding.approveButton.setEnabled(anyChecked);
    }

    private void addDivider()
    {
        View divider = new View(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
        divider.setLayoutParams(params);
        divider.setBackgroundColor(Color.parseColor("#E0E0E0"));
        binding.credentialList.addView(divider);
    }

    private void onApproveClicked()
    {
        Intent intent = new Intent(this, MainActivity.class);

        int selectedCount = 0;
        List<String> selectedHexIds = new ArrayList<>();
        for (Map.Entry<Integer, CheckBox> entry : checkBoxMap.entrySet())
        {
            if (entry.getValue().isChecked())
            {
                Credential cred = emailCredentials.get(entry.getKey());
                byte[] credBytes = cred.toByteArray();
                intent.putExtra(EXTRA_CREDENTIAL_PREFIX + selectedCount, credBytes);
                selectedCount++;
                selectedHexIds.add(Hex.toHexString(cred.getCredential().toByteArray()));
            }
        }

        intent.putExtra(EXTRA_SELECTED_COUNT, selectedCount);
        CredentialStore.saveSelectedCredentials(this, selectedHexIds);
        startActivity(intent);
        finish();
    }
}
