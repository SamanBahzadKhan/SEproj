package com.fridge.caps.views.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
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

    private final List<Counselor>          counselors;
    private final OnCounselorClickListener listener;

    public CounselorAdapter(List<Counselor> counselors, OnCounselorClickListener listener) {
        this.counselors = counselors;
        this.listener   = listener;
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
        holder.tvName.setText(c.getName());
        holder.tvSpecialization.setText(c.getSpecialization());
        holder.tvRating.setText(String.format(Locale.getDefault(), "★ %.1f", c.getRating()));
        holder.tvAccepting.setText(c.isAcceptingClients() ? "Accepting" : "Not Accepting");
        holder.tvAccepting.setTextColor(c.isAcceptingClients()
                ? holder.itemView.getContext().getColor(android.R.color.holo_green_dark)
                : holder.itemView.getContext().getColor(android.R.color.holo_red_dark));
        holder.itemView.setOnClickListener(v -> listener.onCounselorClick(c));
    }

    @Override
    public int getItemCount() { return counselors.size(); }

    static class CounselorViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvSpecialization, tvAccepting, tvRating;
        CounselorViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName           = itemView.findViewById(R.id.tvName);
            tvSpecialization = itemView.findViewById(R.id.tvSpecialization);
            tvAccepting      = itemView.findViewById(R.id.tvAccepting);
            tvRating         = itemView.findViewById(R.id.tvRating);
        }
    }
}