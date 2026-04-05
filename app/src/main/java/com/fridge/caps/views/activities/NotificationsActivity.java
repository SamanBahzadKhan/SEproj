package com.fridge.caps.views.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fridge.caps.R;
import com.fridge.caps.controllers.NotificationController;
import com.fridge.caps.models.Notification;
import com.fridge.caps.views.adapters.NotificationAdapter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Live notifications list; marks visible items read when opened.
 */
public class NotificationsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar  progressBar;
    private TextView     tvEmpty, tvMarkAll;

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

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Notifications");
        }

        tvMarkAll.setOnClickListener(v ->
                notificationController.markAllReadForCurrentUser(null));

        progressBar.setVisibility(View.VISIBLE);
        registration = notificationController.listenToMyNotifications((snap, e) -> {
            progressBar.setVisibility(View.GONE);
            if (e != null || snap == null) {
                tvEmpty.setVisibility(View.VISIBLE);
                return;
            }
            List<Notification> list = new ArrayList<>();
            for (QueryDocumentSnapshot doc : snap) {
                list.add(Notification.fromDocument(doc));
            }
            Collections.sort(list, (a, b) ->
                    Long.compare(b.getTimestampMillis(), a.getTimestampMillis()));
            if (list.isEmpty()) {
                tvEmpty.setVisibility(View.VISIBLE);
                recyclerView.setAdapter(null);
            } else {
                tvEmpty.setVisibility(View.GONE);
                recyclerView.setAdapter(new NotificationAdapter(list,
                        n -> notificationController.markAsRead(n.getNotificationId())));
            }
            notificationController.markAllReadForCurrentUser(null);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (registration != null) {
            registration.remove();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
