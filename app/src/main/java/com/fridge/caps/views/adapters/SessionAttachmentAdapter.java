package com.fridge.caps.views.adapters;


/**
 * Purpose: Handles screen flow, UI state coordination, and user interactions.
 * Depends on: Android UI toolkit, app controllers/viewmodels, and navigation intents.
 * Notes: Focuses on presentation logic while delegating business rules to controllers.
 */
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fridge.caps.R;

import java.util.ArrayList;
import java.util.List;

public class SessionAttachmentAdapter extends RecyclerView.Adapter<SessionAttachmentAdapter.VH> {

    public static class Row {
        public final String displayName;
        public final Uri localUri;
        public final String remoteUrl;

        public Row(String displayName, Uri localUri, String remoteUrl) {
            this.displayName = displayName != null ? displayName : "file";
            this.localUri = localUri;
            this.remoteUrl = remoteUrl;
        }

        public boolean isPendingUpload() {
            return localUri != null;
        }
    }

    private final List<Row> items = new ArrayList<>();
    private final Runnable onChanged;

    public SessionAttachmentAdapter(Runnable onChanged) {
        this.onChanged = onChanged;
    }

    public void setItems(List<Row> next) {
        items.clear();
        items.addAll(next);
        notifyDataSetChanged();
        if (onChanged != null) {
            onChanged.run();
        }
    }

    public List<Row> getItems() {
        return new ArrayList<>(items);
    }

    public void addRow(Row row) {
        items.add(row);
        notifyItemInserted(items.size() - 1);
        if (onChanged != null) {
            onChanged.run();
        }
    }

    public void removeAt(int position) {
        if (position < 0 || position >= items.size()) {
            return;
        }
        items.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, items.size() - position);
        if (onChanged != null) {
            onChanged.run();
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_session_attachment, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Row r = items.get(position);
        h.tvName.setText(r.displayName);
        h.btnRemove.setOnClickListener(v -> {
            int pos = h.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                removeAt(pos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvName;
        final ImageButton btnRemove;

        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvFileName);
            btnRemove = itemView.findViewById(R.id.btnRemoveAttachment);
        }
    }
}
