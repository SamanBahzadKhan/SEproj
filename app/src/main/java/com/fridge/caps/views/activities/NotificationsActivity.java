package com.fridge.caps.views.activities;

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

import java.util.List;

/**
 * NotificationsActivity.java
 * Displays all notifications for the logged-in student (US-9, US-10).
 * View in the MVC pattern.
 */
public class NotificationsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar  progressBar;
    private TextView     tvEmpty, tvMarkAll;

    private NotificationController notificationController;

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

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Notifications");
        }

        loadNotifications();

        tvMarkAll.setOnClickListener(v -> {
            Toast.makeText(this, "All marked as read.", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadNotifications() {
        progressBar.setVisibility(View.VISIBLE);
        notificationController.getMyNotifications(new NotificationController.NotificationListCallback() {
            @Override
            public void onSuccess(List<Notification> notifications) {
                progressBar.setVisibility(View.GONE);
                if (notifications.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                } else {
                    recyclerView.setAdapter(new NotificationAdapter(notifications,
                            notification -> notificationController.markAsRead(
                                    notification.getNotificationId())));
                }
            }

            @Override
            public void onFailure(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(NotificationsActivity.this,
                        "Failed to load notifications.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }
}