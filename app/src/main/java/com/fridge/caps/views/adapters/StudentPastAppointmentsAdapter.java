package com.fridge.caps.views.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.fridge.caps.R;
import com.fridge.caps.models.Appointment;
import com.fridge.caps.models.AppointmentStatus;

import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Student "My Appointments" — past tab rows with optional feedback snippets.
 */
public class StudentPastAppointmentsAdapter extends RecyclerView.Adapter<StudentPastAppointmentsAdapter.VH> {

    public interface Listener {
        void onLeaveFeedback(Appointment a);
    }

    public static final class Snippet {
        public final int rating;
        public final String comment;

        public Snippet(int rating, String comment) {
            this.rating = rating;
            this.comment = comment != null ? comment : "";
        }
    }

    private final List<Appointment> items;
    private final Map<String, Snippet> feedbackByTimeslotId;
    private final Listener listener;

    public StudentPastAppointmentsAdapter(List<Appointment> items,
                                          Map<String, Snippet> feedbackByTimeslotId,
                                          Listener listener) {
        this.items = items;
        this.feedbackByTimeslotId = feedbackByTimeslotId != null
                ? feedbackByTimeslotId
                : Collections.emptyMap();
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_student_my_appt_past, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Appointment a = items.get(position);
        String raw = a.getCounselorName() != null ? a.getCounselorName() : "Counsellor";
        String name = raw.toLowerCase(Locale.US).startsWith("dr.") ? raw : "Dr. " + raw;
        h.tvCounselorName.setText(name);
        String spec = a.getType() != null && !a.getType().isEmpty() ? a.getType() : "";
        h.tvSpecialization.setVisibility(spec.isEmpty() ? View.GONE : View.VISIBLE);
        h.tvSpecialization.setText(spec);
        h.tvDateTime.setText(formatWhen(a));
        h.tvInitials.setText(initialsOf(a.getCounselorName()));

        AppointmentStatus st = a.getStatus() != null ? a.getStatus() : AppointmentStatus.PENDING;
        h.rowStars.setVisibility(View.GONE);
        h.tvReview.setVisibility(View.GONE);
        h.btnWrap.setVisibility(View.GONE);

        if (st == AppointmentStatus.COMPLETED) {
            styleCompletedBadge(h.tvStatusBadge);
            Snippet sn = feedbackByTimeslotId.get(a.getTimeSlotId());
            if (a.isFeedbackSubmitted() && sn != null && sn.rating > 0) {
                h.rowStars.setVisibility(View.VISIBLE);
                bindStars(h.stars, sn.rating);
                if (!sn.comment.isEmpty()) {
                    h.tvReview.setVisibility(View.VISIBLE);
                    h.tvReview.setText(sn.comment);
                }
            } else {
                h.btnWrap.setVisibility(View.VISIBLE);
                h.btnLeaveFeedback.setOnClickListener(v -> {
                    if (listener != null) listener.onLeaveFeedback(a);
                });
            }
        } else if (st == AppointmentStatus.CANCELLED) {
            styleCancelledBadge(h.tvStatusBadge);
        } else if (st == AppointmentStatus.NO_SHOW) {
            styleNoShowBadge(h.tvStatusBadge);
        } else {
            styleCompletedBadge(h.tvStatusBadge);
        }
    }

    private static void bindStars(ImageView[] stars, int rating) {
        int beige = Color.parseColor("#E4C9B6");
        int muted = Color.parseColor("#B7B5AE");
        for (int i = 0; i < 5; i++) {
            if (i < rating) {
                stars[i].setImageResource(R.drawable.ic_star_filled);
                stars[i].setColorFilter(beige, android.graphics.PorterDuff.Mode.SRC_IN);
            } else {
                stars[i].setImageResource(R.drawable.ic_star_outline);
                stars[i].setColorFilter(muted, android.graphics.PorterDuff.Mode.SRC_IN);
            }
        }
    }

    private static void styleCompletedBadge(TextView tv) {
        tv.setText("COMPLETED");
        tv.setBackground(ContextCompat.getDrawable(tv.getContext(), R.drawable.bg_status_completed));
        tv.setTextColor(ContextCompat.getColor(tv.getContext(), R.color.caps_palette_white));
    }

    private static void styleCancelledBadge(TextView tv) {
        tv.setText("CANCELLED");
        tv.setBackground(ContextCompat.getDrawable(tv.getContext(), R.drawable.bg_status_cancelled));
        tv.setTextColor(ContextCompat.getColor(tv.getContext(), R.color.caps_palette_neutral_dark));
    }

    private static void styleNoShowBadge(TextView tv) {
        tv.setText("NO-SHOW");
        tv.setBackground(ContextCompat.getDrawable(tv.getContext(), R.drawable.bg_status_noshow));
        tv.setTextColor(ContextCompat.getColor(tv.getContext(), R.color.caps_palette_neutral_dark));
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
        final TextView tvCounselorName, tvSpecialization, tvDateTime, tvInitials, tvStatusBadge, tvReview;
        final View rowStars, btnWrap;
        final TextView btnLeaveFeedback;
        final ImageView[] stars = new ImageView[5];

        VH(@NonNull View itemView) {
            super(itemView);
            tvCounselorName = itemView.findViewById(R.id.tvCounselorName);
            tvSpecialization = itemView.findViewById(R.id.tvSpecialization);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvInitials = itemView.findViewById(R.id.tvInitials);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
            tvReview = itemView.findViewById(R.id.tvReview);
            rowStars = itemView.findViewById(R.id.rowStars);
            btnWrap = itemView.findViewById(R.id.btnLeaveFeedbackWrap);
            btnLeaveFeedback = itemView.findViewById(R.id.btnLeaveFeedback);
            stars[0] = itemView.findViewById(R.id.star1);
            stars[1] = itemView.findViewById(R.id.star2);
            stars[2] = itemView.findViewById(R.id.star3);
            stars[3] = itemView.findViewById(R.id.star4);
            stars[4] = itemView.findViewById(R.id.star5);
        }
    }
}
