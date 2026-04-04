package com.fridge.caps.views.adapters;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.fridge.caps.R;
import com.fridge.caps.models.Appointment;
import com.fridge.caps.models.AppointmentStatus;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for appointment lists (student, counsellor, admin).
 */
public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.VH> {

    public static final int MODE_STUDENT_UPCOMING = 0;
    public static final int MODE_STUDENT_PAST     = 1;
    public static final int MODE_COUNSELOR        = 2;
    public static final int MODE_ADMIN            = 3;

    private final List<Appointment> items;
    private final int mode;

    @Nullable private final Action onCancel;
    @Nullable private final Action onReschedule;
    @Nullable private final Action onFeedback;
    @Nullable private final Action onComplete;
    @Nullable private final Action onNoShow;

    public interface Action {
        void run(Appointment a);
    }

    public AppointmentAdapter(List<Appointment> items, int mode,
                              @Nullable Action onCancel,
                              @Nullable Action onReschedule,
                              @Nullable Action onFeedback,
                              @Nullable Action onComplete,
                              @Nullable Action onNoShow) {
        this.items     = items;
        this.mode      = mode;
        this.onCancel  = onCancel;
        this.onReschedule = onReschedule;
        this.onFeedback = onFeedback;
        this.onComplete = onComplete;
        this.onNoShow   = onNoShow;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_appointment, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Appointment a = items.get(position);
        AppointmentStatus st = a.getStatus() != null ? a.getStatus() : AppointmentStatus.PENDING;

        String dateTimeStr = formatDateTime(a);
        h.tvDateTime.setText(dateTimeStr);
        String type = a.getType() != null && !a.getType().isEmpty()
                ? a.getType() : "—";
        h.tvType.setText("Type: " + type);

        switch (mode) {
            case MODE_STUDENT_UPCOMING:
                h.tvPrimaryName.setText(a.getCounselorName() != null ? a.getCounselorName() : "Counsellor");
                h.tvSecondaryLine.setVisibility(View.VISIBLE);
                h.tvSecondaryLine.setText("Counselling session");
                h.tvStatusChip.setVisibility(View.VISIBLE);
                h.tvStatusChip.setText("BOOKED");
                styleChip(h.tvStatusChip, Color.parseColor("#4CAF50"));
                h.rowStudentActions.setVisibility(View.VISIBLE);
                h.btnReschedule.setOnClickListener(v -> {
                    if (onReschedule != null) onReschedule.run(a);
                });
                h.btnCancelStudent.setOnClickListener(v -> {
                    if (onCancel != null) onCancel.run(a);
                });
                h.rowCounselorActions.setVisibility(View.GONE);
                h.btnFeedback.setVisibility(View.GONE);
                break;

            case MODE_STUDENT_PAST:
                h.tvPrimaryName.setText(a.getCounselorName() != null ? a.getCounselorName() : "Counsellor");
                h.tvSecondaryLine.setVisibility(View.GONE);
                h.tvStatusChip.setVisibility(View.VISIBLE);
                setStatusUi(h.tvStatusChip, st);
                h.rowStudentActions.setVisibility(View.GONE);
                h.rowCounselorActions.setVisibility(View.GONE);
                boolean showFeedback = st == AppointmentStatus.COMPLETED
                        && !a.isFeedbackSubmitted();
                h.btnFeedback.setVisibility(showFeedback ? View.VISIBLE : View.GONE);
                h.btnFeedback.setOnClickListener(v -> {
                    if (onFeedback != null) onFeedback.run(a);
                });
                if (st == AppointmentStatus.COMPLETED && a.isFeedbackSubmitted()) {
                    h.tvSecondaryLine.setVisibility(View.VISIBLE);
                    h.tvSecondaryLine.setText("Feedback submitted");
                    h.tvSecondaryLine.setTextColor(Color.parseColor("#9E9E9E"));
                }
                break;

            case MODE_COUNSELOR:
                h.tvPrimaryName.setText(a.getStudentName() != null ? a.getStudentName() : "Student");
                h.tvSecondaryLine.setText(a.getTimeDisplay() != null ? a.getTimeDisplay() : "");
                h.tvStatusChip.setText(statusLabel(st));
                styleChip(h.tvStatusChip, colorForStatus(st));
                h.rowStudentActions.setVisibility(View.GONE);
                h.btnFeedback.setVisibility(View.GONE);
                h.rowCounselorActions.setVisibility(View.VISIBLE);
                h.btnComplete.setOnClickListener(v -> {
                    if (onComplete != null) onComplete.run(a);
                });
                h.btnNoShow.setOnClickListener(v -> {
                    if (onNoShow != null) onNoShow.run(a);
                });
                h.btnCancelCounselor.setOnClickListener(v -> {
                    if (onCancel != null) onCancel.run(a);
                });
                break;

            case MODE_ADMIN:
            default:
                h.tvPrimaryName.setText(a.getStudentName() != null ? a.getStudentName() : "Student");
                h.tvSecondaryLine.setText(a.getCounselorName() != null ? a.getCounselorName() : "");
                h.tvStatusChip.setText(statusLabel(st));
                styleChip(h.tvStatusChip, colorForStatus(st));
                h.rowStudentActions.setVisibility(View.GONE);
                h.rowCounselorActions.setVisibility(View.GONE);
                h.btnFeedback.setVisibility(View.GONE);
                break;
        }
    }

    private static String statusLabel(AppointmentStatus st) {
        if (st == null) return "—";
        switch (st) {
            case PENDING:
            case CONFIRMED:
                return "BOOKED";
            case COMPLETED:
                return "COMPLETED";
            case CANCELLED:
                return "CANCELLED";
            case NO_SHOW:
                return "NO-SHOW";
            default:
                return st.name();
        }
    }

    private static int colorForStatus(AppointmentStatus st) {
        if (st == AppointmentStatus.COMPLETED) return Color.parseColor("#4CAF50");
        if (st == AppointmentStatus.CANCELLED) return Color.parseColor("#F44336");
        if (st == AppointmentStatus.NO_SHOW) return Color.parseColor("#9E9E9E");
        return Color.parseColor("#4CAF50");
    }

    private static void setStatusUi(TextView chip, AppointmentStatus st) {
        chip.setVisibility(View.VISIBLE);
        chip.setText(statusLabel(st));
        styleChip(chip, colorForStatus(st));
    }

    private static void styleChip(TextView chip, int bgColor) {
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(24f);
        d.setColor(bgColor);
        chip.setBackground(d);
        chip.setTextColor(Color.WHITE);
    }

    private static String formatDateTime(Appointment a) {
        String time = a.getTimeDisplay() != null ? a.getTimeDisplay() : "";
        Timestamp ts = a.getDate();
        if (ts != null) {
            Date dt = ts.toDate();
            String day = new SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(dt);
            return day + (time.isEmpty() ? "" : " · " + time);
        }
        return time.isEmpty() ? "—" : time;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvPrimaryName, tvSecondaryLine, tvStatusChip, tvDateTime, tvType;
        final View rowStudentActions, rowCounselorActions;
        final View btnReschedule, btnCancelStudent, btnFeedback;
        final View btnComplete, btnNoShow, btnCancelCounselor;

        VH(@NonNull View itemView) {
            super(itemView);
            tvPrimaryName   = itemView.findViewById(R.id.tvPrimaryName);
            tvSecondaryLine = itemView.findViewById(R.id.tvSecondaryLine);
            tvStatusChip    = itemView.findViewById(R.id.tvStatusChip);
            tvDateTime      = itemView.findViewById(R.id.tvDateTime);
            tvType          = itemView.findViewById(R.id.tvType);
            rowStudentActions = itemView.findViewById(R.id.rowStudentActions);
            rowCounselorActions = itemView.findViewById(R.id.rowCounselorActions);
            btnReschedule   = itemView.findViewById(R.id.btnReschedule);
            btnCancelStudent = itemView.findViewById(R.id.btnCancelStudent);
            btnFeedback     = itemView.findViewById(R.id.btnFeedback);
            btnComplete     = itemView.findViewById(R.id.btnComplete);
            btnNoShow       = itemView.findViewById(R.id.btnNoShow);
            btnCancelCounselor = itemView.findViewById(R.id.btnCancelCounselor);
        }
    }
}
