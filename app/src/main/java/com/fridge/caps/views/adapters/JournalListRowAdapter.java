package com.fridge.caps.views.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fridge.caps.R;
import com.fridge.caps.models.JournalEntry;
import com.fridge.caps.utils.DateUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Simple title + date/time list (Journal entries pill). */
public class JournalListRowAdapter extends RecyclerView.Adapter<JournalListRowAdapter.VH> {

    public interface Listener {
        void onRowClick(JournalEntry entry);
    }

    private final List<JournalEntry> items;
    private final Listener listener;

    public JournalListRowAdapter(List<JournalEntry> items, Listener listener) {
        this.items = items;
        this.listener = listener;
    }

    public void update(List<JournalEntry> next) {
        items.clear();
        items.addAll(next);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_journal_list_row, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        JournalEntry e = items.get(position);
        String title = e.getTitle().isEmpty() ? "Untitled" : e.getTitle();
        h.tvTitle.setText(title);
        String datePart = new SimpleDateFormat(DateUtils.DISPLAY_DATE, Locale.getDefault())
                .format(new Date(e.getCreatedAtMillis()));
        String timePart = new SimpleDateFormat(DateUtils.STORAGE_TIME, Locale.getDefault())
                .format(new Date(e.getCreatedAtMillis()));
        h.tvDateTime.setText(datePart + " · " + timePart);
        h.itemView.setOnClickListener(v -> listener.onRowClick(e));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvTitle;
        final TextView tvDateTime;

        VH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvListTitle);
            tvDateTime = itemView.findViewById(R.id.tvListDateTime);
        }
    }
}
