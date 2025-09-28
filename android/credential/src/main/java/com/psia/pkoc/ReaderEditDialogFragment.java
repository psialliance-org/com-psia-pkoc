package com.psia.pkoc;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.psia.pkoc.databinding.DialogEditReaderBinding;

public class ReaderEditDialogFragment extends DialogFragment
{
    public interface ReaderDialogListener
    {
        void onReaderSaved(@NonNull String readerUuid, @NonNull String siteUuid);
    }

    private static final String ARG_READER_UUID = "arg_reader_uuid";
    private static final String ARG_SITE_UUID = "arg_site_uuid";

    public static ReaderEditDialogFragment newInstance(@Nullable String readerUuid,
                                                       @Nullable String siteUuid)
    {
        ReaderEditDialogFragment f = new ReaderEditDialogFragment();
        Bundle b = new Bundle();
        if (!TextUtils.isEmpty(readerUuid)) b.putString(ARG_READER_UUID, readerUuid);
        if (!TextUtils.isEmpty(siteUuid)) b.putString(ARG_SITE_UUID, siteUuid);
        f.setArguments(b);
        return f;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState)
    {
        DialogEditReaderBinding binding = DialogEditReaderBinding.inflate(getLayoutInflater());

        String r = getArguments() != null ? getArguments().getString(ARG_READER_UUID) : null;
        String s = getArguments() != null ? getArguments().getString(ARG_SITE_UUID) : null;

        if (!TextUtils.isEmpty(r)) binding.inputReaderId.setText(r);
        if (!TextUtils.isEmpty(s)) binding.inputReaderSiteId.setText(s);

        androidx.appcompat.app.AlertDialog.Builder b =
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(TextUtils.isEmpty(r) ? "Add Reader" : "Edit Reader")
                .setView(binding.getRoot())
                .setPositiveButton("Save", (d, w) ->
                {
                    String rid = binding.inputReaderId.getText().toString().trim();
                    String sid = binding.inputReaderSiteId.getText().toString().trim();
                    if (getParentFragment() instanceof ReaderDialogListener)
                    {
                        ((ReaderDialogListener) getParentFragment()).onReaderSaved(rid, sid);
                    }
                })
                .setNegativeButton("Cancel", null);

        return b.create();
    }
}
