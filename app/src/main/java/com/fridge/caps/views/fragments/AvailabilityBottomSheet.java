package com.fridge.caps.views.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.fridge.caps.R;
import com.fridge.caps.utils.DateUtils;
import com.fridge.caps.views.activities.CounselorDashboardActivity;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Counsellor sets morning/afternoon availability per date ({@code availability} collection).
 */
public class AvailabilityBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_COUNSELOR_ID = "counselor_id";

    private FirebaseFirestore db;
    private String counselorId;

    private LinearLayout llDateChips;
    private MaterialCardView cardMorning;
    private MaterialCardView cardAfternoon;
    private LinearLayout llScheduledRows;
    private View btnSave;

    private String selectedDate;
    private boolean morningOn;
    private boolean afternoonOn;

    public static AvailabilityBottomSheet newInstance(String counselorId) {
        AvailabilityBottomSheet f = new AvailabilityBottomSheet();
        Bundle b = new Bundle();
        b.putString(ARG_COUNSELOR_ID, counselorId);
        f.setArguments(b);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_availability_bottom_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        if (getArguments() != null) {
            counselorId = getArguments().getString(ARG_COUNSELOR_ID);
        }
        if (counselorId == null && FirebaseAuth.getInstance().getCurrentUser() != null) {
            counselorId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        if (counselorId == null) {
            dismiss();
            return;
        }

        llDateChips = view.findViewById(R.id.llDateChips);
        cardMorning = view.findViewById(R.id.cardMorning);
        cardAfternoon = view.findViewById(R.id.cardAfternoon);
        llScheduledRows = view.findViewById(R.id.llScheduledRows);
        btnSave = view.findViewById(R.id.btnSaveAvailability);

        List<String> days = DateUtils.getNextFourteenDays();
        selectedDate = days.get(0);
        buildDateChips(days);

        cardMorning.setOnClickListener(v -> {
            morningOn = !morningOn;
            applyPeriodStyle(cardMorning, morningOn, true);
        });
        cardAfternoon.setOnClickListener(v -> {
            afternoonOn = !afternoonOn;
            applyPeriodStyle(cardAfternoon, afternoonOn, false);
        });

        loadSelectionForDate(selectedDate);

        btnSave.setOnClickListener(v -> save());

        refreshScheduledList();
    }

    private void buildDateChips(List<String> days) {
        llDateChips.removeAllViews();
        SimpleDateFormat in = new SimpleDateFormat(DateUtils.STORAGE_DATE, Locale.US);
        for (String ymd : days) {
            TextView chip = new TextView(requireContext());
            try {
                Calendar c = Calendar.getInstance(Locale.US);
                c.setTime(in.parse(ymd));
                String dayName = new SimpleDateFormat("EEE", Locale.US).format(c.getTime());
                int dom = c.get(Calendar.DAY_OF_MONTH);
                chip.setText(dayName + "\n" + dom);
            } catch (ParseException e) {
                chip.setText(ymd);
            }
            chip.setPadding(24, 16, 24, 16);
            chip.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            chip.setTag(ymd);
            styleChip(chip, ymd.equals(selectedDate));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(6, 0, 6, 0);
            chip.setLayoutParams(lp);
            chip.setOnClickListener(v -> {
                selectedDate = (String) v.getTag();
                for (int i = 0; i < llDateChips.getChildCount(); i++) {
                    TextView ch = (TextView) llDateChips.getChildAt(i);
                    styleChip(ch, selectedDate.equals(ch.getTag()));
                }
                loadSelectionForDate(selectedDate);
            });
            llDateChips.addView(chip);
        }
    }

    private void styleChip(TextView chip, boolean selected) {
        if (selected) {
            chip.setBackgroundResource(R.drawable.bg_chip_date_selected);
            chip.setTextColor(Color.WHITE);
        } else {
            chip.setBackgroundResource(R.drawable.bg_chip_date_unselected);
            chip.setTextColor(Color.parseColor("#2A4A6B"));
        }
    }

    private void loadSelectionForDate(String date) {
        String docId = counselorId + "_" + date;
        db.collection("availability").document(docId).get()
                .addOnSuccessListener(doc -> {
                    morningOn = doc.exists() && Boolean.TRUE.equals(doc.getBoolean("morning"));
                    afternoonOn = doc.exists() && Boolean.TRUE.equals(doc.getBoolean("afternoon"));
                    applyPeriodStyle(cardMorning, morningOn, true);
                    applyPeriodStyle(cardAfternoon, afternoonOn, false);
                });
    }

    private void applyPeriodStyle(MaterialCardView card, boolean on, boolean morning) {
        if (on) {
            card.setCardBackgroundColor(Color.parseColor("#5BA3D9"));
            card.setStrokeWidth(0);
            setPeriodTextColors(card, Color.WHITE);
        } else {
            card.setCardBackgroundColor(Color.WHITE);
            card.setStrokeWidth((int) (1 * getResources().getDisplayMetrics().density));
            card.setStrokeColor(Color.parseColor("#CCCCCC"));
            setPeriodTextColors(card, Color.parseColor("#888888"));
        }
    }

    private void setPeriodTextColors(MaterialCardView card, int color) {
        ViewGroup root = (ViewGroup) card.getChildAt(0);
        for (int i = 0; i < root.getChildCount(); i++) {
            View child = root.getChildAt(i);
            if (child instanceof TextView) {
                ((TextView) child).setTextColor(color);
            }
        }
    }

    private void save() {
        if (selectedDate == null) {
            Toast.makeText(requireContext(), "Please select a date", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!morningOn && !afternoonOn) {
            Toast.makeText(requireContext(), "Please select at least one period", Toast.LENGTH_SHORT).show();
            return;
        }
        String docId = counselorId + "_" + selectedDate;
        Map<String, Object> data = new HashMap<>();
        data.put("counselorId", counselorId);
        data.put("date", selectedDate);
        data.put("morning", morningOn);
        data.put("afternoon", afternoonOn);
        db.collection("availability").document(docId)
                .set(data)
                .addOnSuccessListener(v -> {
                    Toast.makeText(requireContext(), "Availability saved!", Toast.LENGTH_SHORT).show();
                    refreshScheduledList();
                    if (getActivity() instanceof CounselorDashboardActivity) {
                        ((CounselorDashboardActivity) getActivity()).refreshAvailabilityGrid();
                    }
                    dismiss();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(),
                                e.getMessage() != null ? e.getMessage() : "Save failed",
                                Toast.LENGTH_SHORT).show());
    }

    private void refreshScheduledList() {
        llScheduledRows.removeAllViews();
        String today = DateUtils.getTodayString();
        db.collection("availability")
                .whereEqualTo("counselorId", counselorId)
                .whereGreaterThanOrEqualTo("date", today)
                .orderBy("date")
                .get()
                .addOnSuccessListener(q -> {
                    for (QueryDocumentSnapshot doc : q) {
                        String date = doc.getString("date");
                        boolean m = Boolean.TRUE.equals(doc.getBoolean("morning"));
                        boolean a = Boolean.TRUE.equals(doc.getBoolean("afternoon"));
                        if (!m && !a) continue;
                        View row = buildScheduledRow(doc.getId(), date, m, a);
                        llScheduledRows.addView(row);
                    }
                });
    }

    private View buildScheduledRow(String docId, String dateYmd, boolean morning, boolean afternoon) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 8, 0, 8);

        String label = formatScheduledLabel(dateYmd, morning, afternoon);
        TextView tv = new TextView(requireContext());
        tv.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        tv.setText(label);
        tv.setTextColor(Color.parseColor("#2A4A6B"));
        tv.setTextSize(13);

        TextView del = new TextView(requireContext());
        del.setText(" ✕");
        del.setTextColor(Color.parseColor("#F44336"));
        del.setPadding(16, 0, 0, 0);
        del.setOnClickListener(v -> tryDelete(docId, dateYmd, morning, afternoon));

        row.addView(tv);
        row.addView(del);
        return row;
    }

    private String formatScheduledLabel(String dateYmd, boolean morning, boolean afternoon) {
        String pretty = DateUtils.toDisplayDate(dateYmd);
        String day = DateUtils.getDayAbbreviation(dateYmd);
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(day).append(", ").append(pretty).append("]  ");
        if (morning) sb.append("🌅 Morning  ");
        if (afternoon) sb.append("🌙 Afternoon");
        return sb.toString().trim();
    }

    private void tryDelete(String docId, String dateYmd, boolean morning, boolean afternoon) {
        db.collection("timeslots")
                .whereEqualTo("counselorId", counselorId)
                .whereEqualTo("date", dateYmd)
                .whereEqualTo("isBooked", true)
                .get()
                .addOnSuccessListener(q -> {
                    for (QueryDocumentSnapshot d : q) {
                        String st = d.getString("startTime");
                        String per = d.getString("period");
                        boolean slotMorning = "Morning".equalsIgnoreCase(per)
                                || (per == null && DateUtils.isMorningSlot(st));
                        boolean slotAfternoon = "Afternoon".equalsIgnoreCase(per)
                                || (per == null && st != null && !DateUtils.isMorningSlot(st));
                        if (morning && slotMorning) {
                            Toast.makeText(requireContext(),
                                    "Cannot remove — a student has booked a slot in this period",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }
                        if (afternoon && slotAfternoon) {
                            Toast.makeText(requireContext(),
                                    "Cannot remove — a student has booked a slot in this period",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    db.collection("availability").document(docId)
                            .delete()
                            .addOnSuccessListener(u -> {
                                Toast.makeText(requireContext(), "Removed", Toast.LENGTH_SHORT).show();
                                refreshScheduledList();
                                if (getActivity() instanceof CounselorDashboardActivity) {
                                    ((CounselorDashboardActivity) getActivity()).refreshAvailabilityGrid();
                                }
                            });
                });
    }

    @Override
    public int getTheme() {
        return com.google.android.material.R.style.ThemeOverlay_MaterialComponents_BottomSheetDialog;
    }
}
