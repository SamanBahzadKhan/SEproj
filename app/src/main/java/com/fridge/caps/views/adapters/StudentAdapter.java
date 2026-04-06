package com.fridge.caps.views.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fridge.caps.R;
import com.fridge.caps.models.Student;

import java.util.List;

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
        h.tvEmail.setText(s.getEmail() != null ? s.getEmail() : "—");
        String uid = s.getUserId();
        h.tvId.setText(uid != null && uid.length() >= 8
                ? ("ID: " + uid.substring(0, 8).toUpperCase())
                : "ID: —");
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvName, tvEmail, tvId;

        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            tvId = itemView.findViewById(R.id.tvId);
        }
    }
}