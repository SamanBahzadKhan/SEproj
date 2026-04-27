package com.fridge.caps.views.activities;

/**
 * NotificationsActivity.java
 * Displays user notifications in a list with auto-marking of visible items as read.
 * Shows appointment confirmations, reminders, cancellations, and system messages.
 * View in the MVC pattern.
 */
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Live notifications list; marks visible items read when opened.
 */
public class NotificationsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar  progressBar;
    private TextView     tvEmpty;
    private View         tvMarkAll;

    private NotificationController notificationController;
    private ListenerRegistration   registration;
    private ListenerRegistration   adminRoleRegistration;

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

        tvMarkAll.setOnClickListener(v ->
                notificationController.markAllReadForCurrentUser(null));

        progressBar.setVisibility(View.VISIBLE);
        registration = notificationController.listenToMyNotifications((snap, e) -> {
            progressBar.setVisibility(View.GONE);
            if (e != null || snap == null) {
                tvEmpty.setVisibility(View.VISIBLE);
                return;
            }
            bindNotifications(snap.getDocuments(), null);
            notificationController.markAllReadForCurrentUser(null);
        });

        maybeAttachAdminRoleNotifications();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (registration != null) {
            registration.remove();
        }
        if (adminRoleRegistration != null) {
            adminRoleRegistration.remove();
        }
    }

    private void maybeAttachAdminRoleNotifications() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;
        FirebaseFirestore.getInstance().collection("admins").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    adminRoleRegistration = FirebaseFirestore.getInstance().collection("notifications")
                            .whereEqualTo("targetRole", "admin")
                            .addSnapshotListener((snap, e) -> bindNotifications(
                                    registrationLastDocs, snap != null ? snap.getDocuments() : null));
                });
    }

    private List<DocumentSnapshot> registrationLastDocs = new ArrayList<>();

    private void bindNotifications(List<DocumentSnapshot> userDocs, List<DocumentSnapshot> roleDocs) {
        if (userDocs != null) registrationLastDocs = new ArrayList<>(userDocs);
        List<Notification> list = new ArrayList<>();
        if (registrationLastDocs != null) {
            for (DocumentSnapshot doc : registrationLastDocs) list.add(Notification.fromDocument(doc));
        }
        if (roleDocs != null) {
            for (DocumentSnapshot doc : roleDocs) list.add(Notification.fromDocument(doc));
        }
        Collections.sort(list, (a, b) -> Long.compare(b.getTimestampMillis(), a.getTimestampMillis()));
        if (list.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            recyclerView.setAdapter(null);
        } else {
            tvEmpty.setVisibility(View.GONE);
            recyclerView.setAdapter(new NotificationAdapter(list,
                    this::handleNotificationClick));
        }
    }

    private void handleNotificationClick(Notification n) {
        if (n == null) return;
        if (n.getNotificationId() != null && !n.getNotificationId().isEmpty()) {
            notificationController.markAsRead(n.getNotificationId());
        }
        String type = n.getTypeKey() != null ? n.getTypeKey() : "";
        String reportId = n.getRelatedReportId();
        String signupId = n.getRelatedSignupId();
        if (reportId != null && !reportId.isEmpty()) {
            android.content.Intent i = new android.content.Intent(this, ReportDetailActivity.class);
            i.putExtra(ReportDetailActivity.EXTRA_REPORT_ID, reportId);
            startActivity(i);
            return;
        }
        if (signupId != null && !signupId.isEmpty()) {
            android.content.Intent i = new android.content.Intent(this, PendingCounselorSignupDetailActivity.class);
            i.putExtra(PendingCounselorSignupDetailActivity.EXTRA_SIGNUP_ID, signupId);
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
