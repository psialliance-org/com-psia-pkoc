package com.psia.pkoc;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.psia.pkoc.databinding.ItemSiteBinding;

import org.bouncycastle.util.encoders.Hex;

public class SiteListAdapter extends RecyclerView.Adapter<SiteListAdapter.VH>
{
    public interface OnItemActionListener
    {
        void onEdit(@NonNull SiteModel site);
        void onDelete(@NonNull SiteModel site);
    }

    private final java.util.List<SiteModel> items;
    private final OnItemActionListener listener;

    public SiteListAdapter(java.util.List<SiteModel> items, OnItemActionListener listener)
    {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        ItemSiteBinding b = ItemSiteBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false);
        return new VH(b);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position)
    {
        SiteModel site = items.get(position);
        String uuid = UuidConverters.toUuid(site.SiteUUID).toString();
        String pkHex = Hex.toHexString(site.PublicKey);

        h.binding.txtSiteTitle.setText(uuid);
        h.binding.txtSiteKey.setText(pkHex);

        h.binding.btnEdit.setOnClickListener(v -> listener.onEdit(site));
        h.binding.btnDelete.setOnClickListener(v -> listener.onDelete(site));
    }

    @Override
    public int getItemCount()
    {
        return items == null ? 0 : items.size();
    }

    static class VH extends RecyclerView.ViewHolder
    {
        final ItemSiteBinding binding;

        VH(@NonNull ItemSiteBinding binding)
        {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
