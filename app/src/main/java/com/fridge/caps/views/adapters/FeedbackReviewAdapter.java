package com.fridge.caps.views.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fridge.caps.R;
import com.fridge.caps.models.FeedbackItem;
import com.fridge.caps.utils.RatingDisplayHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Feedback rows on counsellor profile (all reviews, sorted client-side).
 */
public class FeedbackReviewAdapter extends RecyclerView.Adapter<FeedbackReviewAdapter.VH> {

    private final List<FeedbackItem> items = new ArrayList<>();

    public FeedbackReviewAdapter() {}

    public void updateData(List<FeedbackItem> next) {
        items.clear();
        if (next != null) {
            items.addAll(next);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_feedback_review, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        FeedbackItem it = items.get(position);
        h.tvStudentName.setText(it.getStudentName() != null ? it.getStudentName() : "Student");
        h.tvComment.setText(it.getComment() != null ? it.getComment() : "");
        h.tvTime.setText(relativeTime(it.getTimestamp()));
        ImageView[] stars = {h.rs1, h.rs2, h.rs3, h.rs4, h.rs5};
        RatingDisplayHelper.applyReviewStars(stars, it.getRating());
    }

    private static String relativeTime(long base) {
        if (base <= 0) return "";
        long diff = System.currentTimeMillis() - base;
        long minutes = diff / 60000L;
        long hours = diff / 3600000L;
        long days = diff / 86400000L;
        if (minutes < 1) return "Just now";
        if (minutes < 60) return minutes + "m ago";
        if (hours < 24) return hours + "h ago";
        if (days == 1) return "Yesterday";
        return days + " days ago";
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvStudentName, tvComment, tvTime;
        final ImageView rs1, rs2, rs3, rs4, rs5;

        VH(@NonNull View itemView) {
            super(itemView);
            tvStudentName = itemView.findViewById(R.id.tvStudentName);
            tvComment = itemView.findViewById(R.id.tvComment);
            tvTime = itemView.findViewById(R.id.tvTime);
            rs1 = itemView.findViewById(R.id.rs1);
            rs2 = itemView.findViewById(R.id.rs2);
            rs3 = itemView.findViewById(R.id.rs3);
            rs4 = itemView.findViewById(R.id.rs4);
            rs5 = itemView.findViewById(R.id.rs5);
        }
    }
}
