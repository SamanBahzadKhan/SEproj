package com.fridge.caps.views.adapters;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fridge.caps.R;
import com.fridge.caps.models.Appointment;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import com.google.firebase.Timestamp;

/**
 * Pending appointment requests (status PENDING) for counsellor dashboard.
 */
public class PendingRequestAdapter extends RecyclerView.Adapter<PendingRequestAdapter.VH> {

    public interface Action {
        void onConfirm(Appointment a);
        void onDecline(Appointment a);
    }

    private final List<Appointment> items;
    private final Action action;

    public PendingRequestAdapter(List<Appointment> items, Action action) {
        this.items = items;
        this.action = action;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pending_request, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Appointment a = items.get(position);
        h.tvStudentName.setText(a.getStudentName() != null ? a.getStudentName() : "Student");
        String line = formatWhen(a);
        h.tvDateTime.setText(line);
        String type = a.getType() != null ? a.getType() : "—";
        h.tvType.setText(type);
        styleChip(h.tvType, Color.parseColor("#5BA3D9"));

        h.btnConfirm.setOnClickListener(v -> action.onConfirm(a));
        h.btnDecline.setOnClickListener(v -> action.onDecline(a));
    }

    private static String formatWhen(Appointment a) {
        String time = a.getTimeDisplay() != null ? a.getTimeDisplay() : "";
        Timestamp ts = a.getDate();
        if (ts != null) {
            String day = new SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(ts.toDate());
            return day + (time.isEmpty() ? "" : " · " + time);
        }
        return time.isEmpty() ? "—" : time;
    }

    private static void styleChip(TextView chip, int bgColor) {
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(24f);
        d.setColor(bgColor);
        chip.setBackground(d);
        chip.setTextColor(Color.WHITE);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvStudentName, tvDateTime, tvType;
        final View btnConfirm, btnDecline;

        VH(@NonNull View itemView) {
            super(itemView);
            tvStudentName = itemView.findViewById(R.id.tvStudentName);
            tvDateTime    = itemView.findViewById(R.id.tvDateTime);
            tvType        = itemView.findViewById(R.id.tvType);
            btnConfirm    = itemView.findViewById(R.id.btnConfirm);
            btnDecline    = itemView.findViewById(R.id.btnDecline);
        }
    }
}
