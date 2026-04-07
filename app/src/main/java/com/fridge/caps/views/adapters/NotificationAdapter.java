package com.fridge.caps.views.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fridge.caps.R;
import com.fridge.caps.models.Notification;
import com.fridge.caps.models.NotificationType;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Adapter for the notifications list.
 */
public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.VH> {

    public interface OnNotificationClickListener {
        void onNotificationClick(Notification n);
    }

    private final List<Notification> items;
    private final OnNotificationClickListener listener;

    public NotificationAdapter(List<Notification> items, OnNotificationClickListener listener) {
        this.items    = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Notification n = items.get(position);
        h.tvTitle.setText(n.getTitle() != null ? n.getTitle() : "");
        h.tvMessage.setText(n.getMessage() != null ? n.getMessage() : "");
        h.tvTime.setText(relativeTime(n));

        int bar = Color.parseColor("#5BA3D9");
        String icon = "🔔";
        String tk = n.getTypeKey();
        if (tk != null && !tk.isEmpty()) {
            switch (tk) {
                case "CONFIRMATION":
                case "NEW_BOOKING":
                    bar = Color.parseColor("#5BA3D9");
                    icon = "✓";
                    break;
                case "PENDING":
                    bar = Color.parseColor("#FFA000");
                    icon = "⏳";
                    break;
                case "REMINDER":
                    bar = Color.parseColor("#FFA000");
                    icon = "⏰";
                    break;
                case "COMPLETED":
                    bar = Color.parseColor("#4CAF50");
                    icon = "✓";
                    break;
                case "CANCELLED":
                case "CANCELLATION":
                    bar = Color.parseColor("#F44336");
                    icon = "✕";
                    break;
                case "RESCHEDULE":
                    bar = Color.parseColor("#4CAF50");
                    icon = "↻";
                    break;
                case "FEEDBACK":
                    bar = Color.parseColor("#FFC107");
                    icon = "⭐";
                    break;
                default:
                    break;
            }
        } else {
            NotificationType t = n.getType();
            if (t != null) {
                switch (t) {
                    case CONFIRMATION:
                        bar = Color.parseColor("#5BA3D9");
                        break;
                    case REMINDER:
                        bar = Color.parseColor("#FFA000");
                        break;
                    case CANCELLATION:
                        bar = Color.parseColor("#F44336");
                        break;
                    case RESCHEDULE:
                        bar = Color.parseColor("#4CAF50");
                        break;
                    default:
                        break;
                }
            }
        }
        h.barColor.setBackgroundColor(bar);
        h.tvIcon.setText(icon);

        int bg = n.isRead() ? Color.WHITE : Color.parseColor("#EBF5FF");
        h.cardRoot.setCardBackgroundColor(bg);

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onNotificationClick(n);
        });
    }

    private static String relativeTime(Notification n) {
        long base = n.getTimestampMillis();
        if (base <= 0 && n.getSentAt() != null) {
            base = n.getSentAt().toDate().getTime();
        }
        if (base <= 0) return "";
        long diffMs = System.currentTimeMillis() - base;
        long mins = TimeUnit.MILLISECONDS.toMinutes(diffMs);
        if (mins < 1) return "Just now";
        if (mins < 60) return mins + " mins ago";
        long hrs = TimeUnit.MILLISECONDS.toHours(diffMs);
        if (hrs < 24) return hrs + " hours ago";
        long days = TimeUnit.MILLISECONDS.toDays(diffMs);
        return days + " days ago";
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final View barColor;
        final TextView tvIcon, tvTitle, tvMessage, tvTime;
        final com.google.android.material.card.MaterialCardView cardRoot;

        VH(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.cardRoot);
            barColor = itemView.findViewById(R.id.barColor);
            tvIcon   = itemView.findViewById(R.id.tvIcon);
            tvTitle  = itemView.findViewById(R.id.tvTitle);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime   = itemView.findViewById(R.id.tvTime);
        }
    }
}
