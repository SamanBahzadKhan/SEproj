package com.fridge.caps.views.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fridge.caps.R;
import com.fridge.caps.models.PendingCounselorSignup;

import java.util.List;

public class PendingCounselorSignupAdapter extends RecyclerView.Adapter<PendingCounselorSignupAdapter.VH> {
    public interface ActionListener {
        void onOpen(PendingCounselorSignup signup);
        void onApprove(PendingCounselorSignup signup);
        void onReject(PendingCounselorSignup signup);
    }

    private final List<PendingCounselorSignup> items;
    private final ActionListener listener;

    public PendingCounselorSignupAdapter(List<PendingCounselorSignup> items, ActionListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pending_signup, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        PendingCounselorSignup s = items.get(position);
        h.tvName.setText(s.getName() != null ? s.getName() : "Unknown");
        h.tvSub.setText((s.getSpecialization() != null ? s.getSpecialization() : "No specialization")
                + " · " + (s.getEmail() != null ? s.getEmail() : ""));
        h.itemView.setOnClickListener(v -> listener.onOpen(s));
        h.btnApprove.setOnClickListener(v -> listener.onApprove(s));
        h.btnReject.setOnClickListener(v -> listener.onReject(s));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvSub;
        View btnApprove, btnReject;
        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvSignupName);
            tvSub = itemView.findViewById(R.id.tvSignupSub);
            btnApprove = itemView.findViewById(R.id.btnSignupApprove);
            btnReject = itemView.findViewById(R.id.btnSignupReject);
        }
    }
}
