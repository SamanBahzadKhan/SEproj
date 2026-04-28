package com.fridge.caps.views.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.fridge.caps.R;
import com.fridge.caps.controllers.SessionNotesController;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Section headers per counsellor plus one row per received session note document.
 */
public class StudentReceivedNotesListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM   = 1;

    public interface Listener {
        void onOpenNote(DocumentSnapshot doc);
    }

    private final List<Row> rows = new ArrayList<>();
    private final Listener listener;

    public StudentReceivedNotesListAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setDocuments(List<DocumentSnapshot> sortedDocs) {
        rows.clear();
        String lastCounselor = null;
        for (DocumentSnapshot doc : sortedDocs) {
            if (!doc.exists()) {
                continue;
            }
            String counselor = formatCounselorTitle(doc.getString("counselorName"));
            if (lastCounselor == null || !counselor.equalsIgnoreCase(lastCounselor)) {
                rows.add(Row.header(counselor));
                lastCounselor = counselor;
            }
            rows.add(Row.item(doc));
        }
        notifyDataSetChanged();
    }

    private static String formatCounselorTitle(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return "Your counsellor";
        }
        String t = raw.trim();
        if (t.toLowerCase(Locale.US).startsWith("dr.")) {
            return t;
        }
        return "Dr. " + t;
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).header ? TYPE_HEADER : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            View v = inf.inflate(R.layout.item_received_note_header, parent, false);
            return new HeaderVH(v);
        }
        View v = inf.inflate(R.layout.item_received_note_row, parent, false);
        return new ItemVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row row = rows.get(position);
        if (holder instanceof HeaderVH) {
            ((HeaderVH) holder).bind(row.sectionTitle);
        } else if (holder instanceof ItemVH) {
            ((ItemVH) holder).bind(row.doc, listener);
        }
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    private static final class Row {
        final boolean header;
        final String sectionTitle;
        final DocumentSnapshot doc;

        private Row(boolean header, String sectionTitle, DocumentSnapshot doc) {
            this.header = header;
            this.sectionTitle = sectionTitle;
            this.doc = doc;
        }

        static Row header(String title) {
            return new Row(true, title, null);
        }

        static Row item(DocumentSnapshot d) {
            return new Row(false, null, d);
        }
    }

    static class HeaderVH extends RecyclerView.ViewHolder {
        final TextView tv;

        HeaderVH(@NonNull View itemView) {
            super(itemView);
            tv = itemView.findViewById(R.id.tvSectionCounselor);
        }

        void bind(String title) {
            tv.setText(title);
        }
    }

    static class ItemVH extends RecyclerView.ViewHolder {
        final TextView tvSessionLine;
        final TextView tvStatus;

        ItemVH(@NonNull View itemView) {
            super(itemView);
            tvSessionLine = itemView.findViewById(R.id.tvSessionLine);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }

        void bind(DocumentSnapshot doc, Listener listener) {
            String line = doc.getString("sessionDateLine");
            if (line == null || line.isEmpty()) {
                line = "—";
            }
            tvSessionLine.setText(line);
            boolean shared = SessionNotesController.isReceivedNotesVisibleToStudent(doc);
            if (shared) {
                tvStatus.setText(R.string.session_notes_status_shared);
                tvStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.caps_brut_status_teal));
            } else {
                tvStatus.setText(R.string.session_notes_status_pending);
                tvStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.caps_palette_grey_warm));
            }
            itemView.findViewById(R.id.cardInner).setOnClickListener(v -> listener.onOpenNote(doc));
        }
    }
}
