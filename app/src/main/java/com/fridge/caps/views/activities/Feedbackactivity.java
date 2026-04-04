package com.fridge.caps.views.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.fridge.caps.R;
import com.fridge.caps.controllers.FeedbackController;
import com.google.firebase.auth.FirebaseAuth;

/**
 * FeedbackActivity.java
 * Allows students to submit a rating and comment after a session (US-18).
 * View in the MVC pattern.
 */
public class FeedbackActivity extends AppCompatActivity {

    private TextView    tvCounselorName;
    private RatingBar   ratingBar;
    private EditText    etComment;
    private Button      btnSubmit;
    private ProgressBar progressBar;

    private FeedbackController feedbackController;
    private String appointmentId, counselorId, counselorName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        feedbackController = new FeedbackController();

        appointmentId = getIntent().getStringExtra("appointmentId");
        counselorId   = getIntent().getStringExtra("counselorId");
        counselorName = getIntent().getStringExtra("counselorName");

        tvCounselorName = findViewById(R.id.tvCounselorName);
        ratingBar       = findViewById(R.id.ratingBar);
        etComment       = findViewById(R.id.etComment);
        btnSubmit       = findViewById(R.id.btnSubmit);
        progressBar     = findViewById(R.id.progressBar);

        tvCounselorName.setText(counselorName);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Leave Feedback");
        }

        btnSubmit.setOnClickListener(v -> submitFeedback());
    }

    private void submitFeedback() {
        int rating    = (int) ratingBar.getRating();
        String comment = etComment.getText().toString().trim();
        String uid    = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (uid == null) { Toast.makeText(this, "Not logged in.", Toast.LENGTH_SHORT).show(); return; }
        if (rating == 0) { Toast.makeText(this, "Please select a rating.", Toast.LENGTH_SHORT).show(); return; }

        progressBar.setVisibility(View.VISIBLE);
        btnSubmit.setEnabled(false);

        feedbackController.submitFeedback(appointmentId, uid, counselorId,
                rating, comment, new FeedbackController.FeedbackCallback() {
                    @Override
                    public void onSuccess() {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(FeedbackActivity.this,
                                "Feedback submitted. Thank you!", Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onFailure(String error) {
                        progressBar.setVisibility(View.GONE);
                        btnSubmit.setEnabled(true);
                        Toast.makeText(FeedbackActivity.this,
                                "Failed: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }
}