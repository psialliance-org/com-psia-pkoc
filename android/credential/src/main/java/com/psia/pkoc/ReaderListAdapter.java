package com.psia.pkoc;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.psia.pkoc.databinding.ItemReaderBinding;

public class ReaderListAdapter extends RecyclerView.Adapter<ReaderListAdapter.VH>
{
    public interface OnItemActionListener
    {
        void onEdit(@NonNull ReaderModel reader);
        void onDelete(@NonNull ReaderModel reader);
    }

    private final java.util.List<ReaderModel> items;
    private final OnItemActionListener listener;

    public ReaderListAdapter(java.util.List<ReaderModel> items, OnItemActionListener listener)
    {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        ItemReaderBinding b = ItemReaderBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false);
        return new VH(b);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position)
    {
        ReaderModel r = items.get(position);
        String readerUuid = UuidConverters.toUuid(r.getReaderIdentifier()).toString();
        String siteUuid = UuidConverters.toUuid(r.getSiteIdentifier()).toString();

        h.binding.txtReaderTitle.setText(readerUuid);
        h.binding.txtReaderSite.setText(siteUuid);

        h.binding.btnEdit.setOnClickListener(v -> listener.onEdit(r));
        h.binding.btnDelete.setOnClickListener(v -> listener.onDelete(r));
    }

    @Override
    public int getItemCount()
    {
        return items == null ? 0 : items.size();
    }

    static class VH extends RecyclerView.ViewHolder
    {
        final ItemReaderBinding binding;

        VH(@NonNull ItemReaderBinding binding)
        {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}

