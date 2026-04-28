package com.fridge.caps.views.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fridge.caps.R;
import com.fridge.caps.models.Appointment;
import com.fridge.caps.utils.DateUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Completed sessions for student history; same card stack as dashboard past items.
 */
public class StudentCompletedHistoryAdapter extends RecyclerView.Adapter<StudentCompletedHistoryAdapter.VH> {

    public interface Listener {
        void onSessionClick(Appointment appointment);
    }

    private final List<Appointment> items;
    private final Listener listener;

    public StudentCompletedHistoryAdapter(List<Appointment> items, Listener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_student_history_completed, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Appointment a = items.get(position);
        String name = a.getCounselorName() != null && !a.getCounselorName().isEmpty()
                ? a.getCounselorName() : "Counsellor";
        if (!name.toLowerCase(Locale.US).startsWith("dr.")) {
            name = "Dr. " + name;
        }
        h.tvCounselorName.setText(name);
        h.tvSessionWhen.setText(formatSessionLine(a));
        h.itemView.setOnClickListener(v -> listener.onSessionClick(a));
    }

    public static String formatSessionLine(Appointment a) {
        String time = a.getTimeDisplay() != null ? a.getTimeDisplay() : "";
        if (a.getDate() == null) {
            return time.isEmpty() ? "—" : time;
        }
        String day = new SimpleDateFormat(DateUtils.DISPLAY_DATE, Locale.getDefault())
                .format(new Date(a.getDate().toDate().getTime()));
        return time.isEmpty() ? day : day + " · " + time;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvCounselorName;
        final TextView tvSessionWhen;

        VH(@NonNull View itemView) {
            super(itemView);
            tvCounselorName = itemView.findViewById(R.id.tvCounselorName);
            tvSessionWhen = itemView.findViewById(R.id.tvSessionWhen);
        }
    }
}
