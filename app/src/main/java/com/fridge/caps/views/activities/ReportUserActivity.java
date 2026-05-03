package com.fridge.caps.views.activities;


/**
 * Purpose: Handles screen flow, UI state coordination, and user interactions.
 * Depends on: Android UI toolkit, app controllers/viewmodels, and navigation intents.
 * Notes: Focuses on presentation logic while delegating business rules to controllers.
 */
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.fridge.caps.R;
import com.fridge.caps.controllers.ReportController;

public class ReportUserActivity extends AppCompatActivity {
    public static final String EXTRA_REPORTED_USER_ID = "reportedUserId";
    public static final String EXTRA_REPORTED_USER_NAME = "reportedUserName";
    public static final String EXTRA_REPORTED_USER_ROLE = "reportedUserRole";

    private Spinner spinnerType;
    private EditText etSummary;
    private ProgressBar progressBar;
    private ReportController reportController;
    private String reportedUserId;
    private String reportedUserName;
    private String reportedUserRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_user);

        reportController = new ReportController();
        spinnerType = findViewById(R.id.spinnerReportType);
        etSummary = findViewById(R.id.etReportSummary);
        progressBar = findViewById(R.id.progressBar);
        TextView tvReportingName = findViewById(R.id.tvReportingName);
        TextView tvReportingRole = findViewById(R.id.tvReportingRole);
        Button btnSubmit = findViewById(R.id.btnSubmitReport);

        reportedUserId = getIntent().getStringExtra(EXTRA_REPORTED_USER_ID);
        reportedUserName = getIntent().getStringExtra(EXTRA_REPORTED_USER_NAME);
        reportedUserRole = getIntent().getStringExtra(EXTRA_REPORTED_USER_ROLE);

        if (reportedUserId == null || reportedUserName == null || reportedUserRole == null) {
            Toast.makeText(this, "Missing report target.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvReportingName.setText("Reporting: " + reportedUserName);
        tvReportingRole.setText(reportedUserRole.substring(0, 1).toUpperCase()
                + reportedUserRole.substring(1).toLowerCase());

        String[] options = new String[]{"", "Misconduct", "Harassment", "Late", "Unprofessional", "Negligence", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnSubmit.setOnClickListener(v -> submit());
    }

    private void submit() {
        String type = spinnerType.getSelectedItem() != null
                ? spinnerType.getSelectedItem().toString().trim() : "";
        String summary = etSummary.getText() != null ? etSummary.getText().toString().trim() : "";
        if (type.isEmpty()) {
            Toast.makeText(this, "Please select report type.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (summary.isEmpty()) {
            Toast.makeText(this, "Please provide summary.", Toast.LENGTH_SHORT).show();
            return;
        }
        progressBar.setVisibility(View.VISIBLE);
        reportController.submitReport(reportedUserId, reportedUserName, reportedUserRole, type, summary,
                new ReportController.ActionCallback() {
                    @Override
                    public void onSuccess() {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(ReportUserActivity.this,
                                "Report submitted successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onFailure(String error) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(ReportUserActivity.this,
                                error != null ? error : "Failed to submit report", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
