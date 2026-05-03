package com.fridge.caps.views.adapters;

/**
 * CounselorAdapter.java
 * RecyclerView adapter for displaying counselor profile cards with ratings and specializations.
 * Used in counselor list and browsing screens with click listeners for profile navigation.
 * View in the MVC pattern.
 */
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.fridge.caps.R;
import com.fridge.caps.models.Counselor;
import java.util.List;
import java.util.Locale;

/**
 * CounselorAdapter.java
 * RecyclerView adapter for displaying the counselor list (US-3).
 * View in the MVC pattern.
 */
public class CounselorAdapter extends RecyclerView.Adapter<CounselorAdapter.CounselorViewHolder> {

    public interface OnCounselorClickListener {
        void onCounselorClick(Counselor counselor);
    }
    public interface OnCounselorActionListener {
        void onEdit(Counselor counselor);
        void onDelete(Counselor counselor);
    }

    private final List<Counselor>          counselors;
    private final OnCounselorClickListener listener;
    private final OnCounselorActionListener actionListener;

    public CounselorAdapter(List<Counselor> counselors, OnCounselorClickListener listener) {
        this(counselors, listener, null);
    }

    public CounselorAdapter(List<Counselor> counselors, OnCounselorClickListener listener,
                            OnCounselorActionListener actionListener) {
        this.counselors = counselors;
        this.listener   = listener;
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public CounselorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_counselor, parent, false);
        return new CounselorViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull CounselorViewHolder holder, int position) {
        Counselor c = counselors.get(position);
        String name = c.getName() != null ? c.getName() : "";
        if (!name.toLowerCase(Locale.US).startsWith("dr.")) {
            name = "Dr. " + name;
        }
        holder.tvName.setText(name);
        holder.tvSpecialization.setText(c.getSpecialization());
        holder.tvRatingDetail.setText(String.format(Locale.getDefault(), "%.1f (%d reviews)",
                c.getRating(), c.getRatingCount()));
        holder.tvAccepting.setText(c.isAcceptingClients() ? "Accepting" : "Not Accepting");
        holder.tvAccepting.setTextColor(ContextCompat.getColor(holder.itemView.getContext(),
                R.color.caps_palette_white));
        holder.tvAccepting.setBackgroundResource(c.isAcceptingClients()
                ? R.drawable.bg_pill_status_active
                : R.drawable.bg_pill_status_inactive);
        holder.llMiniStars.setVisibility(View.GONE);
        holder.itemView.setOnClickListener(v -> listener.onCounselorClick(c));

        String initials = initialsOf(c.getName());
        holder.tvInitials.setText(initials);
        if (actionListener != null) {
            holder.btnEdit.setVisibility(View.VISIBLE);
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.btnEdit.setOnClickListener(v -> actionListener.onEdit(c));
            holder.btnDelete.setOnClickListener(v -> actionListener.onDelete(c));
        } else {
            holder.btnEdit.setVisibility(View.GONE);
            holder.btnDelete.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() { return counselors.size(); }

    static class CounselorViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvSpecialization, tvAccepting, tvRatingDetail;
        TextView tvInitials;
        View btnEdit, btnDelete;
        LinearLayout llMiniStars;
        final ImageView[] miniStars = new ImageView[5];

        CounselorViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName           = itemView.findViewById(R.id.tvName);
            tvSpecialization = itemView.findViewById(R.id.tvSpecialization);
            tvAccepting      = itemView.findViewById(R.id.tvAccepting);
            tvRatingDetail   = itemView.findViewById(R.id.tvRatingDetail);
            tvInitials       = itemView.findViewById(R.id.tvInitials);
            llMiniStars      = itemView.findViewById(R.id.llMiniStars);
            btnEdit          = itemView.findViewById(R.id.btnCounselorEdit);
            btnDelete        = itemView.findViewById(R.id.btnCounselorDelete);
            miniStars[0] = itemView.findViewById(R.id.sm1);
            miniStars[1] = itemView.findViewById(R.id.sm2);
            miniStars[2] = itemView.findViewById(R.id.sm3);
            miniStars[3] = itemView.findViewById(R.id.sm4);
            miniStars[4] = itemView.findViewById(R.id.sm5);
        }
    }

    private String initialsOf(String name) {
        if (name == null || name.trim().isEmpty()) return "NA";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 0) return "NA";
        if (parts.length == 1) {
            String only = parts[0];
            return only.isEmpty() ? "NA" : only.substring(0, 1).toUpperCase(Locale.US);
        }
        String first = parts[0];
        String last = parts[parts.length - 1];
        if (first.isEmpty() || last.isEmpty()) return "NA";
        return first.substring(0, 1).toUpperCase(Locale.US)
                + last.substring(0, 1).toUpperCase(Locale.US);
    }
}