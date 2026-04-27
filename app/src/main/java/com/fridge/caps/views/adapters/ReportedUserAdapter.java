package com.fridge.caps.views.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fridge.caps.R;
import com.fridge.caps.models.UserReport;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ReportedUserAdapter extends RecyclerView.Adapter<ReportedUserAdapter.VH> {
    public interface ActionListener {
        void onOpen(UserReport report);
        void onRemove(UserReport report);
        void onIgnore(UserReport report);
    }

    private final List<UserReport> items;
    private final ActionListener listener;

    public ReportedUserAdapter(List<UserReport> items, ActionListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reported_user, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        UserReport r = items.get(position);
        h.tvName.setText(value(r.getReportedUserName(), "Unknown User"));
        h.tvType.setText(value(r.getReportType(), "Report"));
        h.tvReporter.setText("Reported by: " + value(r.getReporterUserName(), "Unknown"));
        h.tvSummary.setText(value(r.getReportSummary(), "No summary."));
        if (r.getTimestamp() != null) {
            h.tvTime.setText(new SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.US)
                    .format(r.getTimestamp().toDate()));
        } else {
            h.tvTime.setText("");
        }
        h.itemView.setOnClickListener(v -> listener.onOpen(r));
        h.btnRemove.setOnClickListener(v -> listener.onRemove(r));
        h.btnIgnore.setOnClickListener(v -> listener.onIgnore(r));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String value(String v, String fallback) {
        return v != null && !v.trim().isEmpty() ? v : fallback;
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvType, tvReporter, tvSummary, tvTime;
        View btnRemove, btnIgnore;

        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvReportedName);
            tvType = itemView.findViewById(R.id.tvReportType);
            tvReporter = itemView.findViewById(R.id.tvReporter);
            tvSummary = itemView.findViewById(R.id.tvReportSummary);
            tvTime = itemView.findViewById(R.id.tvReportTime);
            btnRemove = itemView.findViewById(R.id.btnReportRemove);
            btnIgnore = itemView.findViewById(R.id.btnReportIgnore);
        }
    }
}
