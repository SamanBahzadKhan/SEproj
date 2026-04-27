package com.fridge.caps.views.adapters;

/**
 * AppointmentAdapter.java
 * RecyclerView adapter for displaying appointment lists in different contexts (student, counselor, admin).
 * Supports multiple view modes with context-specific action buttons (cancel, reschedule, complete, feedback).
 * View in the MVC pattern.
 */
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
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
public class AppointmentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_STANDARD = 0;
    private static final int TYPE_COUNSELOR_LIST = 1;

    public static final int MODE_STUDENT_UPCOMING = 0;
    public static final int MODE_STUDENT_PAST     = 1;
    public static final int MODE_COUNSELOR        = 2;
    public static final int MODE_ADMIN            = 3;
    /** Read-only list: student name, date/time, type, status (counselor all-appointments screen). */
    public static final int MODE_COUNSELOR_APPOINTMENT_LIST = 4;

    private final List<Appointment> items;
    private final int mode;

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
                              @Nullable Action onNoShow,
                              @Nullable Action onRecordDiagnosis) {
        this.items     = items;
        this.mode      = mode;
        this.onCancel  = onCancel;
        this.onReschedule = onReschedule;
        this.onFeedback = onFeedback;
        this.onComplete = onComplete;
        this.onNoShow   = onNoShow;
        this.onRecordDiagnosis = onRecordDiagnosis;
    }

    @Override
    public int getItemViewType(int position) {
        return mode == MODE_COUNSELOR_APPOINTMENT_LIST ? TYPE_COUNSELOR_LIST : TYPE_STANDARD;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_COUNSELOR_LIST) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_counselor_appointment, parent, false);
            return new CounselorListVH(v);
        }
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_appointment, parent, false);
        return new StandardVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Appointment a = items.get(position);
        if (holder instanceof CounselorListVH) {
            bindCounselorList((CounselorListVH) holder, a);
        } else if (holder instanceof StandardVH) {
            bindStandard((StandardVH) holder, a);
        }
    }

    private void bindCounselorList(CounselorListVH h, Appointment a) {
        String stud = a.getStudentName() != null && !a.getStudentName().isEmpty()
                ? a.getStudentName() : "Student";
        h.tvStudentName.setText(stud);
        h.tvDateTimeLine.setText(formatDateTime(a));

        String typeOnly = a.getType() != null && !a.getType().isEmpty() ? a.getType() : "In-Person";
        h.tvTypeChip.setText(typeOnly);

        counselListStatusChip(h.tvStatusBadge, a);

        AppointmentStatus st = a.getStatus() != null ? a.getStatus() : AppointmentStatus.PENDING;
        boolean completed = st == AppointmentStatus.COMPLETED;
        h.btnRecordDiagnosis.setVisibility(completed ? View.VISIBLE : View.GONE);
        h.btnRecordDiagnosis.setOnClickListener(v -> {
            if (onRecordDiagnosis != null) {
                onRecordDiagnosis.run(a);
            }
        });

        h.tvOverflow.setVisibility(View.VISIBLE);
        h.tvOverflow.setOnClickListener(v -> showOverflowMenu(v, a));
    }

    private static void counselListStatusChip(TextView chip, Appointment a) {
        AppointmentStatus st = a.getStatus() != null ? a.getStatus() : AppointmentStatus.PENDING;
        chip.setVisibility(View.VISIBLE);
        chip.setTypeface(null, Typeface.BOLD);
        chip.setTextSize(10f);
        chip.setTextColor(Color.WHITE);
        if (st == AppointmentStatus.PENDING || st == AppointmentStatus.CONFIRMED) {
            chip.setText("BOOKED");
            chip.setBackground(ContextCompat.getDrawable(chip.getContext(), R.drawable.bg_counselor_list_badge_booked));
            return;
        }
        if (st == AppointmentStatus.COMPLETED) {
            chip.setText("COMPLETED");
            chip.setBackground(ContextCompat.getDrawable(chip.getContext(), R.drawable.bg_status_completed));
            return;
        }
        if (st == AppointmentStatus.CANCELLED) {
            chip.setText("CANCELLED");
            chip.setBackground(ContextCompat.getDrawable(chip.getContext(), R.drawable.bg_status_cancelled));
            chip.setTextColor(Color.WHITE);
            return;
        }
        if (st == AppointmentStatus.NO_SHOW) {
            chip.setText("NO-SHOW");
            chip.setBackground(ContextCompat.getDrawable(chip.getContext(), R.drawable.bg_status_noshow));
            chip.setTextColor(Color.WHITE);
            return;
        }
        chip.setText("BOOKED");
        chip.setBackground(ContextCompat.getDrawable(chip.getContext(), R.drawable.bg_counselor_list_badge_booked));
    }

    private void bindStandard(StandardVH h, Appointment a) {
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

        switch (mode) {
            case MODE_STUDENT_UPCOMING:
                String counselorName = a.getCounselorName() != null ? a.getCounselorName() : "Counsellor";
                if (!counselorName.toLowerCase(Locale.US).startsWith("dr.")) {
                    counselorName = "Dr. " + counselorName;
                }
                h.tvPrimaryName.setText(counselorName);
                h.tvPrimaryName.setTextColor(Color.parseColor("#1E2F3F"));
                h.tvSecondaryLine.setVisibility(View.VISIBLE);
                h.tvSecondaryLine.setText(formatUpcomingDateLine(a));
                h.tvSecondaryLine.setTextColor(Color.parseColor("#3D6D8C"));
                h.tvStatusChip.setVisibility(View.VISIBLE);
                h.tvStatusChip.setText("BOOKED");
                h.tvStatusChip.setBackground(ContextCompat.getDrawable(h.tvStatusChip.getContext(), R.drawable.bg_status_booked));
                h.tvStatusChip.setTextColor(Color.WHITE);
                h.btnReschedule.setVisibility(st == AppointmentStatus.PENDING ? View.GONE : View.VISIBLE);
                h.tvDateTime.setText(formatUpcomingDayNumber(a));
                h.tvDateTime.setTextColor(Color.parseColor("#1E2F3F"));
                h.tvDateTime.setTextSize(56f);
                h.tvType.setText(a.getType() != null && !a.getType().isEmpty() ? a.getType() : "In-Person");
                h.tvType.setTextColor(Color.parseColor("#2D2D2D"));
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

        if (h.tvOverflow != null) {
            boolean showOverflow = mode == MODE_STUDENT_UPCOMING
                    || mode == MODE_STUDENT_PAST
                    || mode == MODE_COUNSELOR;
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

    static class CounselorListVH extends RecyclerView.ViewHolder {
        final TextView tvStudentName;
        final TextView tvDateTimeLine;
        final TextView tvTypeChip;
        final TextView tvStatusBadge;
        final TextView tvOverflow;
        final TextView btnRecordDiagnosis;

        CounselorListVH(@NonNull View itemView) {
            super(itemView);
            tvStudentName = itemView.findViewById(R.id.tvStudentName);
            tvDateTimeLine = itemView.findViewById(R.id.tvDateTimeLine);
            tvTypeChip = itemView.findViewById(R.id.tvTypeChip);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
            tvOverflow = itemView.findViewById(R.id.tvOverflow);
            btnRecordDiagnosis = itemView.findViewById(R.id.btnRecordDiagnosis);
        }
    }

    static class StandardVH extends RecyclerView.ViewHolder {
        final TextView tvPrimaryName, tvSecondaryLine, tvStatusChip, tvDateTime, tvType, tvDurationChip;
        final TextView tvOverflow;
        final View rowStudentActions, rowCounselorActions;
        final View btnReschedule, btnCancelStudent, btnFeedback;
        final View btnComplete, btnNoShow, btnCancelCounselor;

        StandardVH(@NonNull View itemView) {
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
        }
    }
}
