package com.fridge.caps.views.adapters;


/**
 * Purpose: Handles screen flow, UI state coordination, and user interactions.
 * Depends on: Android UI toolkit, app controllers/viewmodels, and navigation intents.
 * Notes: Focuses on presentation logic while delegating business rules to controllers.
 */
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.fridge.caps.R;
import com.fridge.caps.models.Notification;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.VH> {
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    public interface OnNotificationClickListener {
        void onNotificationClick(Notification n);
    }

    
    public interface OnMeetLinkTapListener {
        void onMeetLinkTap(Notification n);
    }

    private final List<Row> rows;
    private final OnNotificationClickListener listener;
    @Nullable private final OnMeetLinkTapListener meetLinkListener;

    public NotificationAdapter(List<Notification> items, OnNotificationClickListener listener) {
        this(items, listener, null);
    }

    public NotificationAdapter(List<Notification> items, OnNotificationClickListener listener,
                               @Nullable OnMeetLinkTapListener meetLinkListener) {
        this.rows = buildRows(items);
        this.listener = listener;
        this.meetLinkListener = meetLinkListener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = viewType == TYPE_HEADER
                ? R.layout.item_notification_header
                : R.layout.item_notification;
        return new VH(LayoutInflater.from(parent.getContext()).inflate(layout, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Row row = rows.get(position);
        if (row.isHeader) {
            h.tvHeader.setText(row.headerLabel);
            return;
        }

        Notification n = row.notification;
        h.tvTitle.setText(n.getTitle() != null ? n.getTitle() : "");
        h.tvMessage.setText(n.getMessage() != null ? n.getMessage() : "");
        h.tvTime.setText(relativeTime(n));
        String tk = n.getTypeKey();
        int iconRes = android.R.drawable.ic_popup_reminder;
        int iconBg = R.drawable.bg_offset_notification_icon_sage;
        int cardBg = R.drawable.bg_offset_notification_card_bluegrey;
        int iconTint = 0xFF2D2D2D;
        if (tk != null) {
            switch (tk) {
                case "REMINDER":
                case "PENDING":
                    iconRes = android.R.drawable.ic_lock_idle_alarm;
                    iconBg = R.drawable.bg_offset_notification_icon_peach;
                    cardBg = R.drawable.bg_offset_notification_card_peach;
                    break;
                case "CANCELLED":
                case "CANCELLATION":
                case "NO_SHOW":
                case "MISSED":
                    iconRes = android.R.drawable.ic_delete;
                    iconBg = R.drawable.bg_offset_notification_icon_rose;
                    cardBg = R.drawable.bg_offset_notification_card_rose;
                    break;
                case "NEW_REPORT":
                    iconRes = android.R.drawable.ic_dialog_alert;
                    iconBg = R.drawable.bg_offset_notification_icon_bluegray;
                    cardBg = R.drawable.bg_offset_notification_card_bluegrey;
                    break;
                case "COMPLETED":
                    iconRes = android.R.drawable.checkbox_on_background;
                    iconBg = R.drawable.bg_offset_notification_icon_sage;
                    cardBg = R.drawable.bg_offset_notification_card_sage;
                    iconTint = 0xFFFFFFFF;
                    break;
                case "meet_link":
                    iconRes = android.R.drawable.ic_media_play;
                    iconBg = R.drawable.bg_offset_notification_icon_bluegray;
                    cardBg = R.drawable.bg_offset_notification_card_bluegrey;
                    iconTint = 0xFFFFFFFF;
                    break;
                case "CONFIRMATION":
                case "NEW_BOOKING":
                default:
                    iconRes = android.R.drawable.checkbox_on_background;
                    iconBg = R.drawable.bg_offset_notification_icon_sage;
                    cardBg = R.drawable.bg_offset_notification_card_bluegrey;
                    iconTint = 0xFFFFFFFF;
                    break;
            }
        }
        h.cardRoot.setBackgroundResource(cardBg);
        h.iconCircle.setBackgroundResource(iconBg);
        h.ivIcon.setImageResource(iconRes);
        h.ivIcon.setColorFilter(iconTint);

        if (h.btnTapJoin != null) {
            String ml = n.getMeetLink();
            boolean showJoin = "meet_link".equals(tk) && ml != null && !ml.trim().isEmpty();
            h.btnTapJoin.setVisibility(showJoin ? View.VISIBLE : View.GONE);
            if (showJoin && meetLinkListener != null) {
                h.btnTapJoin.setOnClickListener(v ->
                        meetLinkListener.onMeetLinkTap(n));
            } else {
                h.btnTapJoin.setOnClickListener(null);
            }
        }

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onNotificationClick(n);
        });
    }

    @Override
    public int getItemCount() { return rows.size(); }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).isHeader ? TYPE_HEADER : TYPE_ITEM;
    }

    private static List<Row> buildRows(List<Notification> src) {
        List<Row> out = new ArrayList<>();
        boolean hasToday = false;
        boolean hasYesterday = false;
        boolean hasEarlier = false;
        for (Notification n : src) {
            if (isToday(n.getTimestampMillis())) {
                if (!hasToday) {
                    out.add(Row.header("TODAY"));
                    hasToday = true;
                }
                out.add(Row.item(n));
            } else if (isYesterday(n.getTimestampMillis())) {
                if (!hasYesterday) {
                    out.add(Row.header("YESTERDAY"));
                    hasYesterday = true;
                }
                out.add(Row.item(n));
            } else {
                if (!hasEarlier) {
                    out.add(Row.header("EARLIER"));
                    hasEarlier = true;
                }
                out.add(Row.item(n));
            }
        }
        return out;
    }

    private static boolean isToday(long millis) {
        if (millis <= 0) return false;
        String a = new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date(millis));
        String b = new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());
        return a.equals(b);
    }

    private static boolean isYesterday(long millis) {
        if (millis <= 0) return false;
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.add(java.util.Calendar.DAY_OF_YEAR, -1);
        String y = new SimpleDateFormat("yyyyMMdd", Locale.US).format(c.getTime());
        String a = new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date(millis));
        return y.equals(a);
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

    static class VH extends RecyclerView.ViewHolder {
        final View cardRoot;
        final View iconCircle;
        final ImageView ivIcon;
        final TextView tvTitle, tvMessage, tvTime, tvHeader;
        @Nullable final TextView btnTapJoin;

        VH(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.cardRoot);
            iconCircle = itemView.findViewById(R.id.iconCircle);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvHeader = itemView.findViewById(R.id.tvHeader);
            btnTapJoin = itemView.findViewById(R.id.btnTapJoin);
        }
    }

    private static class Row {
        final boolean isHeader;
        final String headerLabel;
        final Notification notification;

        private Row(boolean isHeader, String headerLabel, Notification notification) {
            this.isHeader = isHeader;
            this.headerLabel = headerLabel;
            this.notification = notification;
        }

        static Row header(String label) { return new Row(true, label, null); }
        static Row item(Notification n) { return new Row(false, null, n); }
    }
}