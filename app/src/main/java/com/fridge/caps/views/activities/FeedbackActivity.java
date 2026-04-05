package com.fridge.caps.views.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.fridge.caps.R;
import com.fridge.caps.controllers.FeedbackController;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Star rating + comment; saves to {@code feedback} and updates timeslot.
 */
public class FeedbackActivity extends AppCompatActivity {

    public static final String EXTRA_TIMESLOT_ID            = "timeslotId";
    public static final String EXTRA_COUNSELOR_ID           = "counselorId";
    public static final String EXTRA_COUNSELOR_NAME         = "counselorName";
    public static final String EXTRA_COUNSELOR_SPECIALIZATION = "counselorSpecialization";
    public static final String EXTRA_APPOINTMENT_DATE       = "appointmentDate";

    private TextView       tvCounselorName, tvSpecialization, tvAppointmentDate;
    private EditText       etComment;
    private MaterialButton btnSubmit;
    private ProgressBar progressBar;
    private ImageView[] stars;
    private int         selectedRating;

    private FeedbackController feedbackController;
    private String timeslotId, counselorId, counselorName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        feedbackController = new FeedbackController();

        timeslotId   = getIntent().getStringExtra(EXTRA_TIMESLOT_ID);
        counselorId  = getIntent().getStringExtra(EXTRA_COUNSELOR_ID);
        counselorName = getIntent().getStringExtra(EXTRA_COUNSELOR_NAME);
        String spec  = getIntent().getStringExtra(EXTRA_COUNSELOR_SPECIALIZATION);
        String date  = getIntent().getStringExtra(EXTRA_APPOINTMENT_DATE);

        tvCounselorName    = findViewById(R.id.tvCounselorName);
        tvSpecialization   = findViewById(R.id.tvSpecialization);
        tvAppointmentDate  = findViewById(R.id.tvAppointmentDate);
        etComment          = findViewById(R.id.etComment);
        btnSubmit          = findViewById(R.id.btnSubmit);
        progressBar        = findViewById(R.id.progressBar);
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

        btnSubmit.setOnClickListener(v -> submitFeedback());
    }

    private void setRating(int rating) {
        selectedRating = rating;
        int yellow = 0xFFFFC107;
        int grey   = 0xFF9E9E9E;
        for (int i = 0; i < stars.length; i++) {
            if (i < rating) {
                stars[i].setImageResource(android.R.drawable.btn_star_big_on);
                stars[i].setColorFilter(yellow, android.graphics.PorterDuff.Mode.SRC_IN);
            } else {
                stars[i].setImageResource(android.R.drawable.btn_star_big_off);
                stars[i].setColorFilter(grey, android.graphics.PorterDuff.Mode.SRC_IN);
            }
        }
    }

    private void submitFeedback() {
        if (selectedRating <= 0) {
            Toast.makeText(this, "Please select a rating", Toast.LENGTH_SHORT).show();
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

        progressBar.setVisibility(View.VISIBLE);
        btnSubmit.setEnabled(false);

        feedbackController.submitFeedback(timeslotId, uid, counselorId, selectedRating, comment,
                new FeedbackController.FeedbackCallback() {
                    @Override
                    public void onSuccess() {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(FeedbackActivity.this,
                                "Feedback submitted successfully", Toast.LENGTH_SHORT).show();
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
    }
}
