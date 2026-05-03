package com.fridge.caps.views.activities;

/**
 * NotificationsActivity.java
 * Displays user notifications in a list (unread only). Mark-all-read clears them from this screen.
 */
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fridge.caps.R;
import com.fridge.caps.controllers.NotificationController;
import com.fridge.caps.models.Notification;
import com.fridge.caps.views.adapters.NotificationAdapter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Live unread notifications; Mark all read dismisses the list until new notifications arrive.
 */
public class NotificationsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar  progressBar;
    private TextView     tvEmpty;
    private View         tvMarkAll;

    private NotificationController notificationController;
    private ListenerRegistration   registration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        notificationController = new NotificationController();

        recyclerView = findViewById(R.id.recyclerView);
        progressBar  = findViewById(R.id.progressBar);
        tvEmpty      = findViewById(R.id.tvEmpty);
        tvMarkAll    = findViewById(R.id.tvMarkAll);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        tvMarkAll.setOnClickListener(v -> {
            tvMarkAll.setEnabled(false);
            notificationController.markAllReadForCurrentUser(() -> runOnUiThread(() -> {
                tvMarkAll.setEnabled(true);
                bindNotifications(Collections.emptyList());
            }));
        });

        progressBar.setVisibility(View.VISIBLE);
        registration = notificationController.listenToMyNotifications((snap, e) -> {
            progressBar.setVisibility(View.GONE);
            if (e != null || snap == null) {
                tvEmpty.setVisibility(View.VISIBLE);
                recyclerView.setAdapter(null);
                return;
            }
            bindNotifications(snap.getDocuments());
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (registration != null) {
            registration.remove();
        }
    }

    private void bindNotifications(List<DocumentSnapshot> docs) {
        List<Notification> list = new ArrayList<>();
        if (docs != null) {
            for (DocumentSnapshot doc : docs) {
                list.add(Notification.fromDocument(doc));
            }
        }
        Collections.sort(list, (a, b) -> Long.compare(b.getTimestampMillis(), a.getTimestampMillis()));
        if (list.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            recyclerView.setAdapter(null);
        } else {
            tvEmpty.setVisibility(View.GONE);
            recyclerView.setAdapter(new NotificationAdapter(list, this::handleNotificationClick,
                    this::handleMeetLinkTap));
        }
    }

    private void handleMeetLinkTap(Notification n) {
        if (n == null) return;
        if (n.getNotificationId() != null && !n.getNotificationId().isEmpty()) {
            notificationController.markAsRead(n.getNotificationId());
        }
        String url = n.getMeetLink();
        if (url != null && !url.trim().isEmpty()) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url.trim())));
            } catch (Exception e) {
                Toast.makeText(this, "Could not open meeting link.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void handleNotificationClick(Notification n) {
        if (n == null) return;
        if (n.getNotificationId() != null && !n.getNotificationId().isEmpty()) {
            notificationController.markAsRead(n.getNotificationId());
        }
        String type = n.getTypeKey() != null ? n.getTypeKey() : "";
        String reportId = n.getRelatedReportId();
        if (reportId != null && !reportId.isEmpty()) {
            android.content.Intent i = new android.content.Intent(this, ReportDetailActivity.class);
            i.putExtra(ReportDetailActivity.EXTRA_REPORT_ID, reportId);
            startActivity(i);
            return;
        }
        if ("NEW_REPORT".equals(type) || "COUNSELLOR_SIGNUP_PENDING".equals(type)) {
            startActivity(new android.content.Intent(this, AdminDashboardActivity.class));
            return;
        }
        Toast.makeText(this, n.getTitle(), Toast.LENGTH_SHORT).show();
    }
}
