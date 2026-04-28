package com.fridge.caps.views.adapters;

/**
 * AppointmentAdapter.java
 * RecyclerView adapter for displaying appointment lists in different contexts (student, counselor, admin).
 * Supports multiple view modes with context-specific action buttons (cancel, reschedule, complete, feedback).
 * View in the MVC pattern.
 */
import android.animation.AnimatorInflater;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.fridge.caps.R;
import com.fridge.caps.models.Appointment;
import com.fridge.caps.models.AppointmentStatus;
import com.fridge.caps.views.activities.ReportUserActivity;
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
    /** Read-only list: student name, date/time, type, status (counselor all-appointments screen). */
    public static final int MODE_COUNSELOR_APPOINTMENT_LIST = 4;

    private final List<Appointment> items;
    private final int mode;
    private final int itemLayoutRes;

    @Nullable private final Action onCancel;
    @Nullable private final Action onReschedule;
    @Nullable private final Action onFeedback;
    @Nullable private final Action onComplete;
    @Nullable private final Action onNoShow;
    @Nullable private final Action onRecordDiagnosis;

    public interface Action {
        void run(Appointment a);
    }

    public AppointmentAdapter(List<Appointment> items, int mode,
                              @Nullable Action onCancel,
                              @Nullable Action onReschedule,
                              @Nullable Action onFeedback,
                              @Nullable Action onComplete,
                              @Nullable Action onNoShow) {
        this(items, mode, onCancel, onReschedule, onFeedback, onComplete, onNoShow, null);
    }

    public AppointmentAdapter(List<Appointment> items, int mode,
                              @Nullable Action onCancel,
                              @Nullable Action onReschedule,
                              @Nullable Action onFeedback,
                              @Nullable Action onComplete,
                              @Nullable Action onNoShow,
                              @Nullable Action onRecordDiagnosis) {
        this.items     = items;
        this.mode      = mode;
        this.itemLayoutRes = mode == MODE_STUDENT_UPCOMING
                ? R.layout.item_appointment_student_upcoming_brut
                : R.layout.item_appointment;
        this.onCancel  = onCancel;
        this.onReschedule = onReschedule;
        this.onFeedback = onFeedback;
        this.onComplete = onComplete;
        this.onNoShow   = onNoShow;
        this.onRecordDiagnosis = onRecordDiagnosis;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(itemLayoutRes, parent, false);
        VH h = new VH(v);
        if (mode == MODE_STUDENT_UPCOMING && android.os.Build.VERSION.SDK_INT
                >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            h.itemView.setStateListAnimator(AnimatorInflater.loadStateListAnimator(
                    parent.getContext(), R.animator.brut_card_interaction));
        }
        return h;
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Appointment a = items.get(position);
        AppointmentStatus st = a.getStatus() != null ? a.getStatus() : AppointmentStatus.PENDING;

        h.tvPrimaryName.setTypeface(null, Typeface.NORMAL);
        h.tvPrimaryName.setTextColor(Color.parseColor("#2A4A6B"));
        h.tvDateTime.setVisibility(View.VISIBLE);
        h.tvDateTime.setTextSize(13f);

        String dateTimeStr = formatDateTime(a);
        h.tvDateTime.setText(dateTimeStr);
        String type = a.getType() != null && !a.getType().isEmpty()
                ? a.getType() : "—";
        h.tvType.setText("Type: " + type);
        h.tvDurationChip.setVisibility(View.VISIBLE);
        if (h.rowRecordDiagnosis != null) {
            h.rowRecordDiagnosis.setVisibility(View.GONE);
        }

        switch (mode) {
            case MODE_STUDENT_UPCOMING: {
                int navy = ContextCompat.getColor(h.itemView.getContext(), R.color.caps_brut_navy);
                int blueGray = ContextCompat.getColor(h.itemView.getContext(), R.color.caps_brut_blue_gray);
                String counselorName = a.getCounselorName() != null ? a.getCounselorName() : "Counsellor";
                if (!counselorName.toLowerCase(Locale.US).startsWith("dr.")) {
                    counselorName = "Dr. " + counselorName;
                }
                h.tvPrimaryName.setText(counselorName);
                h.tvPrimaryName.setTextColor(navy);
                h.tvSecondaryLine.setVisibility(View.VISIBLE);
                h.tvSecondaryLine.setText(formatUpcomingDateLine(a));
                h.tvSecondaryLine.setTextColor(blueGray);
                h.tvStatusChip.setVisibility(View.VISIBLE);
                if (st == AppointmentStatus.PENDING) {
                    h.tvStatusChip.setText("PENDING");
                    h.tvStatusChip.setBackground(ContextCompat.getDrawable(h.itemView.getContext(),
                            R.drawable.bg_brut_status_pending_pill));
                    h.tvStatusChip.setTextColor(navy);
                } else {
                    h.tvStatusChip.setText("BOOKED");
                    h.tvStatusChip.setBackground(ContextCompat.getDrawable(h.itemView.getContext(),
                            R.drawable.bg_brut_status_booked_pill));
                    h.tvStatusChip.setTextColor(ContextCompat.getColor(h.itemView.getContext(),
                            R.color.caps_palette_white));
                }
                h.btnReschedule.setVisibility(View.VISIBLE);
                h.tvDateTime.setText(formatUpcomingDayNumber(a));
                h.tvDateTime.setTextColor(navy);
                h.tvDateTime.setTextSize(56f);
                h.tvType.setText(a.getType() != null && !a.getType().isEmpty() ? a.getType() : "In-Person");
                h.tvType.setTextColor(navy);
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
            }

            case MODE_STUDENT_PAST:
                h.tvPrimaryName.setText(a.getCounselorName() != null ? a.getCounselorName() : "Counsellor");
                h.tvSecondaryLine.setVisibility(View.GONE);
                h.tvStatusChip.setVisibility(View.VISIBLE);
                setPastStatusUi(h.tvStatusChip, a);
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
                h.tvPrimaryName.setText(a.getTimeDisplay() != null ? a.getTimeDisplay() : "");
                h.tvPrimaryName.setTextColor(Color.parseColor("#8A8680"));
                h.tvPrimaryName.setTypeface(null, Typeface.NORMAL);
                h.tvSecondaryLine.setText(a.getStudentName() != null ? a.getStudentName() : "Student");
                h.tvSecondaryLine.setTextColor(Color.parseColor("#2D2D2D"));
                h.tvDateTime.setVisibility(View.GONE);
                String apType = a.getType() != null && !a.getType().isEmpty()
                        ? a.getType() : "—";
                h.tvStatusChip.setText(apType);
                h.tvStatusChip.setBackground(ContextCompat.getDrawable(h.tvStatusChip.getContext(),
                        R.drawable.bg_offset_chip_small));
                h.tvStatusChip.setTextColor(Color.parseColor("#2D2D2D"));
                h.rowStudentActions.setVisibility(View.GONE);
                h.btnFeedback.setVisibility(View.GONE);
                h.rowCounselorActions.setVisibility(View.VISIBLE);
                h.btnNoShow.setVisibility(View.GONE);
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

            case MODE_COUNSELOR_APPOINTMENT_LIST: {
                String stud = a.getStudentName() != null && !a.getStudentName().isEmpty()
                        ? a.getStudentName() : "Student";
                h.tvPrimaryName.setText(stud);
                h.tvPrimaryName.setTextColor(Color.parseColor("#1E2F3F"));
                h.tvPrimaryName.setTypeface(null, Typeface.BOLD);
                h.tvDateTime.setVisibility(View.GONE);
                h.tvSecondaryLine.setVisibility(View.VISIBLE);
                h.tvSecondaryLine.setText(formatDateTime(a));
                h.tvSecondaryLine.setTextColor(Color.parseColor("#8A8680"));
                String typeOnly = a.getType() != null && !a.getType().isEmpty() ? a.getType() : "—";
                h.tvType.setText(typeOnly);
                listModeStatusChip(h.tvStatusChip, a);
                h.tvDurationChip.setVisibility(View.GONE);
                h.rowStudentActions.setVisibility(View.GONE);
                h.rowCounselorActions.setVisibility(View.GONE);
                h.btnFeedback.setVisibility(View.GONE);
                boolean showRecord = st == AppointmentStatus.COMPLETED && onRecordDiagnosis != null;
                if (h.rowRecordDiagnosis != null) {
                    h.rowRecordDiagnosis.setVisibility(showRecord ? View.VISIBLE : View.GONE);
                }
                if (h.btnRecordDiagnosis != null) {
                    h.btnRecordDiagnosis.setOnClickListener(v -> {
                        if (onRecordDiagnosis != null) {
                            onRecordDiagnosis.run(a);
                        }
                    });
                }
                break;
            }

            case MODE_ADMIN:
            default:
                h.tvPrimaryName.setText(a.getStudentName() != null ? a.getStudentName() : "Student");
                h.tvSecondaryLine.setText(a.getCounselorName() != null ? a.getCounselorName() : "");
                h.tvStatusChip.setText(statusLabel(st));
                styleChip(h.tvStatusChip, colorForStatus(st));
                h.rowStudentActions.setVisibility(View.GONE);
                h.rowCounselorActions.setVisibility(View.GONE);
                h.btnFeedback.setVisibility(View.GONE);
                if (h.rowRecordDiagnosis != null) {
                    h.rowRecordDiagnosis.setVisibility(View.GONE);
                }
                break;
        }

        if (h.tvOverflow != null) {
            boolean showOverflow = mode == MODE_STUDENT_UPCOMING
                    || mode == MODE_STUDENT_PAST
                    || mode == MODE_COUNSELOR
                    || mode == MODE_COUNSELOR_APPOINTMENT_LIST;
            h.tvOverflow.setVisibility(showOverflow ? View.VISIBLE : View.GONE);
            if (showOverflow) {
                h.tvOverflow.setOnClickListener(v -> showOverflowMenu(v, a));
            }
        }
    }

    private void showOverflowMenu(View anchor, Appointment a) {
        PopupMenu popup = new PopupMenu(anchor.getContext(), anchor);
        boolean counselorSide = mode == MODE_COUNSELOR || mode == MODE_COUNSELOR_APPOINTMENT_LIST;
        String reportTitle = anchor.getContext().getString(
                counselorSide ? R.string.report_to_admin : R.string.report_user_menu);
        popup.getMenu().add(reportTitle);
        popup.setOnMenuItemClickListener(item -> {
            if (reportTitle.contentEquals(String.valueOf(item.getTitle()))) {
                if (counselorSide
                        && (a.getStudentId() == null || a.getStudentId().isEmpty())) {
                    Toast.makeText(anchor.getContext(), R.string.report_student_unavailable,
                            Toast.LENGTH_SHORT).show();
                    return true;
                }
                android.content.Intent i = new android.content.Intent(anchor.getContext(), ReportUserActivity.class);
                if (mode == MODE_COUNSELOR || mode == MODE_COUNSELOR_APPOINTMENT_LIST) {
                    i.putExtra(ReportUserActivity.EXTRA_REPORTED_USER_ID, a.getStudentId());
                    i.putExtra(ReportUserActivity.EXTRA_REPORTED_USER_NAME,
                            a.getStudentName() != null && !a.getStudentName().isEmpty()
                                    ? a.getStudentName() : "Student");
                    i.putExtra(ReportUserActivity.EXTRA_REPORTED_USER_ROLE, "student");
                } else {
                    i.putExtra(ReportUserActivity.EXTRA_REPORTED_USER_ID, a.getCounselorId());
                    i.putExtra(ReportUserActivity.EXTRA_REPORTED_USER_NAME, a.getCounselorName());
                    i.putExtra(ReportUserActivity.EXTRA_REPORTED_USER_ROLE, "counsellor");
                }
                anchor.getContext().startActivity(i);
                return true;
            }
            return false;
        });
        popup.show();
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

    private static void listModeStatusChip(TextView chip, Appointment a) {
        AppointmentStatus st = a.getStatus() != null ? a.getStatus() : AppointmentStatus.PENDING;
        chip.setVisibility(View.VISIBLE);
        chip.setTypeface(null, Typeface.BOLD);
        if (st == AppointmentStatus.PENDING || st == AppointmentStatus.CONFIRMED) {
            chip.setText("BOOKED");
            chip.setBackground(ContextCompat.getDrawable(chip.getContext(), R.drawable.bg_status_booked));
            chip.setTextColor(Color.WHITE);
            return;
        }
        setPastStatusUi(chip, a);
    }

    private static void setPastStatusUi(TextView chip, Appointment appt) {
        AppointmentStatus st = appt.getStatus() != null ? appt.getStatus() : AppointmentStatus.PENDING;
        chip.setVisibility(View.VISIBLE);
        chip.setTypeface(null, Typeface.BOLD);
        if (st == AppointmentStatus.NO_SHOW) {
            chip.setText("NO-SHOW");
            chip.setBackground(ContextCompat.getDrawable(chip.getContext(), R.drawable.bg_status_noshow));
            chip.setTextColor(Color.parseColor("#2D2D2D"));
            return;
        }
        if (st == AppointmentStatus.CANCELLED) {
            chip.setText("CANCELLED");
            chip.setBackground(ContextCompat.getDrawable(chip.getContext(), R.drawable.bg_status_cancelled));
            chip.setTextColor(Color.parseColor("#2D2D2D"));
            return;
        }
        if (st == AppointmentStatus.COMPLETED) {
            if (!appt.isFeedbackSubmitted()) {
                chip.setText("FEEDBACK");
                chip.setBackground(ContextCompat.getDrawable(chip.getContext(), R.drawable.bg_status_feedback));
            } else {
                chip.setText("COMPLETED");
                chip.setBackground(ContextCompat.getDrawable(chip.getContext(), R.drawable.bg_status_completed));
            }
            chip.setTextColor(Color.WHITE);
            return;
        }
        chip.setText("DONE");
        if (st == AppointmentStatus.CONFIRMED || st == AppointmentStatus.PENDING) {
            chip.setBackground(ContextCompat.getDrawable(chip.getContext(), R.drawable.bg_status_done));
            chip.setTextColor(Color.WHITE);
            return;
        }
        chip.setBackground(ContextCompat.getDrawable(chip.getContext(), R.drawable.bg_status_feedback));
        chip.setTextColor(Color.WHITE);
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

    private static String formatUpcomingDayNumber(Appointment a) {
        Timestamp ts = a.getDate();
        if (ts == null) return "—";
        return new SimpleDateFormat("d", Locale.getDefault()).format(ts.toDate());
    }

    private static String formatUpcomingDateLine(Appointment a) {
        Timestamp ts = a.getDate();
        if (ts == null) return a.getTimeDisplay() != null ? a.getTimeDisplay() : "";
        String day = new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(ts.toDate());
        String time = a.getTimeDisplay() != null ? a.getTimeDisplay() : "";
        return day + (time.isEmpty() ? "" : " · " + time);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvPrimaryName, tvSecondaryLine, tvStatusChip, tvDateTime, tvType, tvDurationChip;
        final TextView tvOverflow;
        final View rowStudentActions, rowCounselorActions;
        final View btnReschedule, btnCancelStudent, btnFeedback;
        final View btnComplete, btnNoShow, btnCancelCounselor;
        @Nullable final View rowRecordDiagnosis;
        @Nullable final View btnRecordDiagnosis;

        VH(@NonNull View itemView) {
            super(itemView);
            tvPrimaryName   = itemView.findViewById(R.id.tvPrimaryName);
            tvSecondaryLine = itemView.findViewById(R.id.tvSecondaryLine);
            tvStatusChip    = itemView.findViewById(R.id.tvStatusChip);
            tvDateTime      = itemView.findViewById(R.id.tvDateTime);
            tvType          = itemView.findViewById(R.id.tvType);
            tvDurationChip  = itemView.findViewById(R.id.tvDurationChip);
            tvOverflow      = itemView.findViewById(R.id.tvOverflow);
            rowStudentActions = itemView.findViewById(R.id.rowStudentActions);
            rowCounselorActions = itemView.findViewById(R.id.rowCounselorActions);
            btnReschedule   = itemView.findViewById(R.id.btnReschedule);
            btnCancelStudent = itemView.findViewById(R.id.btnCancelStudent);
            btnFeedback     = itemView.findViewById(R.id.btnFeedback);
            btnComplete     = itemView.findViewById(R.id.btnComplete);
            btnNoShow       = itemView.findViewById(R.id.btnNoShow);
            btnCancelCounselor = itemView.findViewById(R.id.btnCancelCounselor);
            rowRecordDiagnosis = itemView.findViewById(R.id.rowRecordDiagnosis);
            btnRecordDiagnosis = itemView.findViewById(R.id.btnRecordDiagnosis);
        }
    }
}