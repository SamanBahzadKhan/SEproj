package com.fridge.caps.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.fridge.caps.R;
import com.fridge.caps.controllers.CounselorController;
import com.fridge.caps.models.Counselor;

/**
 * CounselorProfileActivity.java
 * Displays a counselor's full profile (US-4).
 * Receives counselor ID via Intent and fetches data from Firestore.
 * View in the MVC pattern.
 */
public class CounselorProfileActivity extends AppCompatActivity {

    public static final String EXTRA_COUNSELOR_ID = "counselor_id";

    private TextView    tvName, tvSpecialization, tvBio, tvAvailability, tvEmail;
    private RatingBar   ratingBar;
    private Button      btnViewSlots;
    private ProgressBar progressBar;

    private CounselorController counselorController;
    private String counselorId;
    private String loadedCounselorName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_counselor_profile);

        counselorController = new CounselorController();
        counselorId = getIntent().getStringExtra(EXTRA_COUNSELOR_ID);

        if (counselorId == null || counselorId.isEmpty()) {
            Toast.makeText(this, "Invalid counselor.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvName           = findViewById(R.id.tvName);
        tvSpecialization = findViewById(R.id.tvSpecialization);
        tvBio            = findViewById(R.id.tvBio);
        tvAvailability   = findViewById(R.id.tvAvailability);
        tvEmail          = findViewById(R.id.tvEmail);
        ratingBar        = findViewById(R.id.ratingBar);
        btnViewSlots     = findViewById(R.id.btnViewSlots);
        progressBar      = findViewById(R.id.progressBar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Counselor Profile");
        }

        loadProfile();

        btnViewSlots.setOnClickListener(v -> {
            Intent intent = new Intent(this, TimeSlotsActivity.class);
            intent.putExtra(TimeSlotsActivity.EXTRA_COUNSELOR_ID, counselorId);
            if (loadedCounselorName != null) {
                intent.putExtra(TimeSlotsActivity.EXTRA_COUNSELOR_NAME, loadedCounselorName);
            }
            startActivity(intent);
        });
    }

    private void loadProfile() {
        progressBar.setVisibility(View.VISIBLE);
        btnViewSlots.setEnabled(false);

        counselorController.getCounselorProfile(counselorId,
                new CounselorController.CounselorCallback() {
                    @Override
                    public void onSuccess(Counselor counselor) {
                        progressBar.setVisibility(View.GONE);
                        btnViewSlots.setEnabled(true);
                        populateUI(counselor);
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(CounselorProfileActivity.this,
                                "Failed to load profile: " + errorMessage,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void populateUI(Counselor counselor) {
        loadedCounselorName = counselor.getName();
        tvName.setText(counselor.getName());
        tvSpecialization.setText(counselor.getSpecialization());
        tvBio.setText(counselor.getBio());
        tvEmail.setText(counselor.getEmail());
        ratingBar.setRating(counselor.getRating());

        if (counselor.isAcceptingClients()) {
            tvAvailability.setText("Currently accepting new clients");
            tvAvailability.setTextColor(getColor(android.R.color.holo_green_dark));
        } else {
            tvAvailability.setText("Not currently accepting new clients");
            tvAvailability.setTextColor(getColor(android.R.color.holo_red_dark));
            btnViewSlots.setEnabled(false);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
