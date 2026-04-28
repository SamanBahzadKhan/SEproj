package com.fridge.caps.views.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
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

/**
 * Recent journal entry cards (My Journal screen).
 */
public class JournalEntryCardAdapter extends RecyclerView.Adapter<JournalEntryCardAdapter.VH> {

    public interface Listener {
        void onEdit(JournalEntry entry);

        void onOpen(JournalEntry entry);
    }

    private final List<JournalEntry> items;
    private final Listener listener;

    public JournalEntryCardAdapter(List<JournalEntry> items, Listener listener) {
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
                .inflate(R.layout.item_journal_entry_card, parent, false);
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
        h.tvDate.setText(datePart + " · " + timePart);
        String body = e.getBody().trim();
        h.tvPreview.setText(body.isEmpty() ? "No preview." : body);
        h.tvMoodEmoji.setText(emojiForMood(e.getMood()));

        View.OnClickListener open = v -> listener.onOpen(e);
        h.itemView.setOnClickListener(open);
        h.btnEdit.setOnClickListener(v -> listener.onEdit(e));
    }

    private static String emojiForMood(String mood) {
        if (JournalEntry.MOOD_HAPPY.equals(mood)) {
            return "😊";
        }
        if (JournalEntry.MOOD_SAD.equals(mood)) {
            return "😔";
        }
        return "😐";
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvMoodEmoji, tvTitle, tvDate, tvPreview;
        final ImageButton btnEdit;

        VH(@NonNull View itemView) {
            super(itemView);
            tvMoodEmoji = itemView.findViewById(R.id.tvMoodEmoji);
            tvTitle = itemView.findViewById(R.id.tvEntryTitle);
            tvDate = itemView.findViewById(R.id.tvEntryDate);
            tvPreview = itemView.findViewById(R.id.tvEntryPreview);
            btnEdit = itemView.findViewById(R.id.btnEntryEdit);
        }
    }
}
