package com.fridge.caps.views.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.fridge.caps.R;
import com.fridge.caps.models.Appointment;
import com.fridge.caps.models.AppointmentStatus;

import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * Student "My Appointments" — upcoming tab rows.
 */
public class StudentUpcomingAppointmentsAdapter extends RecyclerView.Adapter<StudentUpcomingAppointmentsAdapter.VH> {

    private final List<Appointment> items;

    public StudentUpcomingAppointmentsAdapter(List<Appointment> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_student_my_appt_upcoming, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Appointment a = items.get(position);
        String raw = a.getCounselorName() != null ? a.getCounselorName() : "Counsellor";
        String name = raw.toLowerCase(Locale.US).startsWith("dr.") ? raw : "Dr. " + raw;
        h.tvCounselorName.setText(name);
        String apType = a.getType() != null && !a.getType().isEmpty() ? a.getType() : "";
        if (apType.isEmpty()) {
            h.tvSpecialization.setVisibility(View.GONE);
        } else {
            h.tvSpecialization.setVisibility(View.VISIBLE);
            h.tvSpecialization.setText(apType);
        }
        h.tvDateTime.setText(formatWhen(a));
        h.tvInitials.setText(initialsOf(a.getCounselorName()));
        h.tvStatusBadge.setBackground(ContextCompat.getDrawable(h.itemView.getContext(),
                R.drawable.bg_status_booked));
        h.tvStatusBadge.setTextColor(ContextCompat.getColor(h.itemView.getContext(),
                R.color.caps_palette_white));
        AppointmentStatus st = a.getStatus() != null ? a.getStatus() : AppointmentStatus.PENDING;
        h.tvStatusBadge.setText(st == AppointmentStatus.PENDING ? "PENDING" : "BOOKED");
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private static String formatWhen(Appointment a) {
        String time = a.getTimeDisplay() != null ? a.getTimeDisplay() : "";
        Timestamp ts = a.getDate();
        if (ts != null) {
            String day = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(ts.toDate());
            return day + (time.isEmpty() ? "" : " · " + time);
        }
        return time.isEmpty() ? "—" : time;
    }

    private static String initialsOf(String name) {
        if (name == null || name.trim().isEmpty()) return "?";
        String n = name.replaceFirst("(?i)^dr\\.\\s*", "").trim();
        String[] parts = n.split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase(Locale.US);
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1))
                .toUpperCase(Locale.US);
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvCounselorName, tvSpecialization, tvDateTime, tvInitials, tvStatusBadge;

        VH(@NonNull View itemView) {
            super(itemView);
            tvCounselorName = itemView.findViewById(R.id.tvCounselorName);
            tvSpecialization = itemView.findViewById(R.id.tvSpecialization);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvInitials = itemView.findViewById(R.id.tvInitials);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
        }
    }
}
