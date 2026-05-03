package com.fridge.caps.views.activities;


/**
 * Purpose: Handles screen flow, UI state coordination, and user interactions.
 * Depends on: Android UI toolkit, app controllers/viewmodels, and navigation intents.
 * Notes: Focuses on presentation logic while delegating business rules to controllers.
 */
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.fridge.caps.R;
import com.fridge.caps.controllers.ReportController;
import com.fridge.caps.models.UserReport;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class ReportDetailActivity extends AppCompatActivity {
    public static final String EXTRA_REPORT_ID = "reportId";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_detail);

        String reportId = getIntent().getStringExtra(EXTRA_REPORT_ID);
        if (reportId == null || reportId.isEmpty()) {
            Toast.makeText(this, "Missing report.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        TextView tvTitle = findViewById(R.id.tvDetailTitle);
        TextView tvMeta = findViewById(R.id.tvDetailMeta);
        TextView tvSummary = findViewById(R.id.tvDetailSummary);

        new ReportController().getReportById(reportId, new ReportController.ReportCallback() {
            @Override
            public void onSuccess(UserReport r) {
                tvTitle.setText(r.getReportedUserName() + " · " + r.getReportType());
                String time = "";
                if (r.getTimestamp() != null) {
                    time = new SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.US)
                            .format(r.getTimestamp().toDate());
                }
                tvMeta.setText("Reported by: " + safe(r.getReporterUserName())
                        + "\nRole: " + safe(r.getReportedUserRole())
                        + "\nStatus: " + safe(r.getStatus())
                        + (time.isEmpty() ? "" : "\n" + time));
                tvSummary.setText(safe(r.getReportSummary()));
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(ReportDetailActivity.this,
                        error != null ? error : "Failed to load report.", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private String safe(String v) { return v != null ? v : "—"; }
}
