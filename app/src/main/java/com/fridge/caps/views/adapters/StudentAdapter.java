package com.fridge.caps.views.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.fridge.caps.R;
import com.fridge.caps.models.Student;

import java.util.List;
import java.util.Locale;

/**
 * Admin student list rows (neubrutalist card + status pill).
 */
public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.VH> {

    private final List<Student> items;

    public StudentAdapter(List<Student> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_student, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Student s = items.get(position);
        h.tvName.setText(s.getName() != null ? s.getName() : "—");
        h.tvStudentId.setText(formatStudentSecondaryLine(s));

        String dept = s.getDepartment() != null ? s.getDepartment().trim() : "";
        String yr = s.getYearOfStudy() != null ? s.getYearOfStudy().trim() : "";
        String deptYear;
        if (!dept.isEmpty() && !yr.isEmpty()) {
            deptYear = dept + " · " + yr;
        } else if (!dept.isEmpty()) {
            deptYear = dept;
        } else if (!yr.isEmpty()) {
            deptYear = yr;
        } else {
            deptYear = "—";
        }
        h.tvDeptYear.setText(deptYear);

        h.tvInitials.setText(initialsOf(s.getName()));

        boolean active = s.isAccountActive();
        h.tvStatusPill.setText(active ? "Active" : "Inactive");
        h.tvStatusPill.setTextColor(ContextCompat.getColor(h.itemView.getContext(),
                R.color.caps_palette_white));
        h.tvStatusPill.setBackgroundResource(active
                ? R.drawable.bg_pill_status_active
                : R.drawable.bg_pill_status_inactive);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * Same idea as the student profile screen: prefer campus {@code studentId}, then email,
     * then a short UID prefix — not the full Firebase UID string.
     */
    private static String formatStudentSecondaryLine(Student s) {
        String campus = s.getCampusStudentId();
        if (!campus.isEmpty()) {
            return campus;
        }
        String email = s.getEmail() != null ? s.getEmail().trim() : "";
        if (!email.isEmpty()) {
            return email;
        }
        String uid = s.getUserId();
        if (uid != null && uid.length() >= 8) {
            return uid.substring(0, 8).toUpperCase(Locale.US);
        }
        return uid != null ? uid : "—";
    }

    private static String initialsOf(String name) {
        if (name == null || name.trim().isEmpty()) return "—";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase(Locale.US);
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1))
                .toUpperCase(Locale.US);
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvName, tvStudentId, tvDeptYear, tvInitials, tvStatusPill;

        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvStudentId = itemView.findViewById(R.id.tvStudentId);
            tvDeptYear = itemView.findViewById(R.id.tvDeptYear);
            tvInitials = itemView.findViewById(R.id.tvInitials);
            tvStatusPill = itemView.findViewById(R.id.tvStatusPill);
        }
    }
}
