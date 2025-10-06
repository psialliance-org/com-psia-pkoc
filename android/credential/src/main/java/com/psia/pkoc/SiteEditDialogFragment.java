package com.psia.pkoc;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.psia.pkoc.databinding.DialogEditSiteBinding;

public class SiteEditDialogFragment extends DialogFragment
{
    public interface SiteDialogListener
    {
        void onSiteSaved(@NonNull String siteUuid, @NonNull String publicKeyHex);
    }

    private static final String ARG_SITE_UUID = "arg_site_uuid";
    private static final String ARG_PUBKEY_HEX = "arg_pubkey_hex";

    public static SiteEditDialogFragment newInstance(@Nullable String siteUuid,
                                                     @Nullable String pubKeyHex)
    {
        SiteEditDialogFragment f = new SiteEditDialogFragment();
        Bundle b = new Bundle();
        if (!TextUtils.isEmpty(siteUuid)) b.putString(ARG_SITE_UUID, siteUuid);
        if (!TextUtils.isEmpty(pubKeyHex)) b.putString(ARG_PUBKEY_HEX, pubKeyHex);
        f.setArguments(b);
        return f;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState)
    {
        DialogEditSiteBinding binding = DialogEditSiteBinding.inflate(getLayoutInflater());

        String siteUuid = getArguments() != null ? getArguments().getString(ARG_SITE_UUID) : null;
        String pkHex = getArguments() != null ? getArguments().getString(ARG_PUBKEY_HEX) : null;

        if (!TextUtils.isEmpty(siteUuid)) binding.inputSiteId.setText(siteUuid);
        if (!TextUtils.isEmpty(pkHex)) binding.inputSitePubkey.setText(pkHex);

        androidx.appcompat.app.AlertDialog.Builder b =
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(TextUtils.isEmpty(siteUuid) ? "Add Site" : "Edit Site")
                .setView(binding.getRoot())
                .setPositiveButton("Save", (d, w) ->
                {
                    String id = binding.inputSiteId.getText().toString().trim();
                    String hex = binding.inputSitePubkey.getText().toString().trim();
                    if (getParentFragment() instanceof SiteDialogListener)
                    {
                        ((SiteDialogListener) getParentFragment()).onSiteSaved(id, hex);
                    }
                })
                .setNegativeButton("Cancel", null);

        return b.create();
    }
}
