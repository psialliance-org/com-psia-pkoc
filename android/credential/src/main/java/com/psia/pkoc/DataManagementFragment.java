package com.psia.pkoc;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.psia.pkoc.databinding.FragmentDataManagementBinding;

import org.bouncycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DataManagementFragment extends Fragment
    implements SiteEditDialogFragment.SiteDialogListener,
    ReaderEditDialogFragment.ReaderDialogListener
{
    private FragmentDataManagementBinding binding;

    private final List<SiteModel> siteItems = new ArrayList<>();
    private final List<ReaderModel> readerItems = new ArrayList<>();

    private SiteListAdapter siteAdapter;
    private ReaderListAdapter readerAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState)
    {
        binding = FragmentDataManagementBinding.inflate(inflater, container, false);

        // Adapters
        siteAdapter = new SiteListAdapter(siteItems, new SiteListAdapter.OnItemActionListener()
        {
            @Override
            public void onEdit(@NonNull SiteModel site)
            {
                String siteUuid = UuidConverters.toUuid(site.SiteUUID).toString();
                String pkHex = Hex.toHexString(site.PublicKey);
                SiteEditDialogFragment.newInstance(siteUuid, pkHex)
                                      .show(getChildFragmentManager(), "edit_site");
            }

            @Override
            public void onDelete(@NonNull SiteModel site)
            {
                confirmDeleteSite(site);
            }
        });

        readerAdapter = new ReaderListAdapter(readerItems, new ReaderListAdapter.OnItemActionListener()
        {
            @Override
            public void onEdit(@NonNull ReaderModel reader)
            {
                String readerUuid = UuidConverters.toUuid(reader.getReaderIdentifier()).toString();
                String siteUuid = UuidConverters.toUuid(reader.getSiteIdentifier()).toString();
                ReaderEditDialogFragment.newInstance(readerUuid, siteUuid)
                                        .show(getChildFragmentManager(), "edit_reader");
            }

            @Override
            public void onDelete(@NonNull ReaderModel reader)
            {
                confirmDeleteReader(reader);
            }
        });

        binding.siteRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.siteRecycler.setAdapter(siteAdapter);

        binding.readerRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.readerRecycler.setAdapter(readerAdapter);

        binding.btnAddSite.setOnClickListener(v ->
            SiteEditDialogFragment.newInstance(null, null)
                                  .show(getChildFragmentManager(), "add_site"));

        binding.btnAddReader.setOnClickListener(v ->
            ReaderEditDialogFragment.newInstance(null, null)
                                    .show(getChildFragmentManager(), "add_reader"));

        reloadAll();

        return binding.getRoot();
    }

    @Override
    public void onSiteSaved(@NonNull String siteUuidString, @NonNull String pubKeyHex)
    {
        if (isValidUuid(siteUuidString))
        {
            toast("Invalid Site UUID.");
            return;
        }
        if (!isValidHex(pubKeyHex))
        {
            toast("Public key must be 65 bytes (130 hex chars).");
            return;
        }

        byte[] siteId = UuidConverters.fromUuid(UUID.fromString(siteUuidString));
        byte[] pubKey = Hex.decode(pubKeyHex);

        PKOC_Application.getDb().getQueryExecutor().execute(() ->
        {
            PKOC_Application.getDb().siteDao().upsert(new SiteModel(siteId, pubKey));
            runUi(() -> { toast("Site saved."); reloadSites(); });
        });
    }

    @Override
    public void onReaderSaved(@NonNull String readerUuidString, @NonNull String siteUuidString)
    {
        if (isValidUuid(readerUuidString))
        {
            toast("Invalid Reader UUID.");
            return;
        }
        if (isValidUuid(siteUuidString))
        {
            toast("Invalid Site UUID for Reader.");
            return;
        }

        byte[] rid = UuidConverters.fromUuid(UUID.fromString(readerUuidString));
        byte[] sid = UuidConverters.fromUuid(UUID.fromString(siteUuidString));

        PKOC_Application.getDb().getQueryExecutor().execute(() ->
        {
            PKOC_Application.getDb().readerDao().upsert(new ReaderModel(rid, sid));
            runUi(() -> { toast("Reader saved."); reloadReaders(); });
        });
    }

    private void confirmDeleteSite(@NonNull SiteModel site)
    {
        new AlertDialog.Builder(requireContext())
            .setTitle("Delete Site")
            .setMessage("Delete this site? Readers referencing it may become orphaned unless you cascade delete.")
            .setPositiveButton("Delete", (d, w) ->
            {
                byte[] sid = site.SiteUUID;
                PKOC_Application.getDb().getQueryExecutor().execute(() ->
                {
                    PKOC_Application.getDb().siteDao().delete(sid);
                    runUi(() -> { toast("Site deleted."); reloadSites(); });
                });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void confirmDeleteReader(@NonNull ReaderModel reader)
    {
        new AlertDialog.Builder(requireContext())
            .setTitle("Delete Reader")
            .setMessage("Delete this reader?")
            .setPositiveButton("Delete", (d, w) ->
            {
                byte[] sid = reader.getSiteIdentifier();
                byte[] rid = reader.getReaderIdentifier();
                PKOC_Application.getDb().getQueryExecutor().execute(() ->
                {
                    PKOC_Application.getDb().readerDao().deleteByIdentity(sid, rid);
                    runUi(() -> { toast("Reader deleted."); reloadReaders(); });
                });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    // ---------- Loaders ----------

    private void reloadAll()
    {
        reloadSites();
        reloadReaders();
    }

    private void reloadSites()
    {
        PKOC_Application.getDb().getQueryExecutor().execute(() ->
        {
            List<SiteModel> sites = PKOC_Application.getDb().siteDao().list();
            runUi(() ->
            {
                siteItems.clear();
                siteItems.addAll(sites);
                siteAdapter.notifyDataSetChanged();
                binding.emptySites.setVisibility(siteItems.isEmpty() ? View.VISIBLE : View.GONE);
            });
        });
    }

    private void reloadReaders()
    {
        PKOC_Application.getDb().getQueryExecutor().execute(() ->
        {
            List<ReaderModel> readers = PKOC_Application.getDb().readerDao().list();
            runUi(() ->
            {
                readerItems.clear();
                readerItems.addAll(readers);
                readerAdapter.notifyDataSetChanged();
                binding.emptyReaders.setVisibility(readerItems.isEmpty() ? View.VISIBLE : View.GONE);
            });
        });
    }

    // ---------- Helpers ----------

    private boolean isValidUuid(@Nullable String s)
    {
        if (TextUtils.isEmpty(s)) return true;
        try
        {
            UUID.fromString(s);
            return false;
        }
        catch (IllegalArgumentException ex)
        {
            return true;
        }
    }

    private boolean isValidHex(@Nullable String s)
    {
        if (TextUtils.isEmpty(s)) return false;
        if (s.length() != 65 * 2) return false;
        return s.matches("^[0-9A-Fa-f]+$");
    }

    private void toast(@NonNull String msg)
    {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    }

    private void runUi(@NonNull Runnable r)
    {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(r);
    }
}
