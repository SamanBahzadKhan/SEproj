package com.fridge.caps.views.activities;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.fridge.caps.R;
import com.fridge.caps.controllers.FeedbackController;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Star rating + comment; saves to {@code feedback} and updates timeslot.
 */
public class FeedbackActivity extends AppCompatActivity {

    public static final String EXTRA_TIMESLOT_ID = "timeslotId";
    public static final String EXTRA_APPOINTMENT_ID = "appointmentId";
    public static final String EXTRA_COUNSELOR_ID = "counselorId";
    public static final String EXTRA_COUNSELOR_NAME = "counselorName";
    public static final String EXTRA_COUNSELOR_SPECIALIZATION = "counselorSpecialization";
    public static final String EXTRA_APPOINTMENT_DATE = "appointmentDate";

    private TextView tvCounselorName, tvSpecialization, tvAppointmentDate;
    private EditText etComment;
    private View btnSubmit;
    private ProgressBar progressBar;
    private ImageView[] stars;
    private int selectedRating;

    private FeedbackController feedbackController;
    private String timeslotId, counselorId, counselorName;
    private boolean isTestMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        feedbackController = new FeedbackController();

        timeslotId = getIntent().getStringExtra(EXTRA_TIMESLOT_ID);
        if (timeslotId == null || timeslotId.isEmpty()) {
            timeslotId = getIntent().getStringExtra(EXTRA_APPOINTMENT_ID);
        }
        counselorId = getIntent().getStringExtra(EXTRA_COUNSELOR_ID);
        counselorName = getIntent().getStringExtra(EXTRA_COUNSELOR_NAME);
        String spec = getIntent().getStringExtra(EXTRA_COUNSELOR_SPECIALIZATION);
        String date = getIntent().getStringExtra(EXTRA_APPOINTMENT_DATE);
        isTestMode = getIntent().getBooleanExtra("TEST_MODE", false);

        tvCounselorName = findViewById(R.id.tvCounselorName);
        tvSpecialization = findViewById(R.id.tvSpecialization);
        tvAppointmentDate = findViewById(R.id.tvAppointmentDate);
        etComment = findViewById(R.id.etComment);
        btnSubmit = findViewById(R.id.btnSubmit);
        progressBar = findViewById(R.id.progressBar);
        ImageButton btnBack = findViewById(R.id.btnBack);

        stars = new ImageView[]{
                findViewById(R.id.star1),
                findViewById(R.id.star2),
                findViewById(R.id.star3),
                findViewById(R.id.star4),
                findViewById(R.id.star5),
        };

        tvCounselorName.setText(counselorName != null ? counselorName : "");
        tvAppointmentDate.setText(date != null && !date.isEmpty() ? date : "");

        if (spec != null && !spec.isEmpty()) {
            tvSpecialization.setText(spec);
        } else if (counselorId != null) {
            FirebaseFirestore.getInstance().collection("counselors").document(counselorId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists() && doc.getString("specialization") != null) {
                            tvSpecialization.setText(doc.getString("specialization"));
                        } else {
                            tvSpecialization.setText("");
                        }
                    });
        } else {
            tvSpecialization.setText("");
        }

        btnBack.setOnClickListener(v -> finish());

        for (int i = 0; i < stars.length; i++) {
            final int rating = i + 1;
            stars[i].setOnClickListener(v -> setRating(rating));
        }

        setRating(0);
        btnSubmit.setOnClickListener(v -> submitFeedback());
    }

    private void setRating(int rating) {
        selectedRating = rating;
        int white = Color.WHITE;
        int dark = ContextCompat.getColor(this, R.color.caps_palette_neutral_dark);
        for (int i = 0; i < stars.length; i++) {
            boolean on = i < rating;
            stars[i].setBackgroundResource(on
                    ? R.drawable.bg_star_rating_cell_filled
                    : R.drawable.bg_star_rating_cell_empty);
            stars[i].setImageResource(on ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);
            stars[i].setImageTintList(ColorStateList.valueOf(on ? white : dark));
        }
    }

    private void submitFeedback() {
        if (selectedRating <= 0) {
            Toast.makeText(this, "Please select a rating", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isTestMode) {
            Toast.makeText(this, "Feedback submitted", Toast.LENGTH_SHORT).show();
            return;
        }
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) {
            Toast.makeText(this, "Not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (timeslotId == null || timeslotId.isEmpty()) {
            Toast.makeText(this, "Missing session.", Toast.LENGTH_SHORT).show();
            return;
        }

        String comment = etComment.getText() != null ? etComment.getText().toString().trim() : "";

        if (counselorId == null || counselorId.isEmpty()) {
            Toast.makeText(this, "Missing counsellor for feedback.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSubmit.setEnabled(false);

        FirebaseFirestore.getInstance().collection("students").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    String studentName = doc.getString("name") != null
                            ? doc.getString("name") : "Student";
                    feedbackController.submitFeedback(timeslotId, uid, studentName, counselorId,
                            selectedRating, comment,
                            new FeedbackController.FeedbackCallback() {
                                @Override
                                public void onSuccess() {
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(FeedbackActivity.this,
                                            "Feedback submitted", Toast.LENGTH_SHORT).show();
                                    finish();
                                }

                                @Override
                                public void onFailure(String error) {
                                    progressBar.setVisibility(View.GONE);
                                    btnSubmit.setEnabled(true);
                                    Toast.makeText(FeedbackActivity.this,
                                            error, Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnSubmit.setEnabled(true);
                    Toast.makeText(this, "Could not load your name.", Toast.LENGTH_SHORT).show();
                });
    }
}
