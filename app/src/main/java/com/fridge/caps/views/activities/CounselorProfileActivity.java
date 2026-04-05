package com.fridge.caps.views.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.fridge.caps.R;
import com.fridge.caps.controllers.CounselorController;
import com.fridge.caps.models.Counselor;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Counsellor profile for students/admins; own profile adds accepting switch + sign out.
 */
public class CounselorProfileActivity extends AppCompatActivity {

    public static final String EXTRA_COUNSELOR_ID   = "counselor_id";
    public static final String EXTRA_COUNSELOR_NAME = "counselor_name";

    private com.google.android.material.button.MaterialButton btnViewSlots;
    private com.google.android.material.button.MaterialButton btnSignOut;
    private SwitchCompat switchAccepting;
    private View cardOwnProfile;
    private android.widget.ProgressBar progressBar;
    private android.widget.TextView tvName, tvSpecialization, tvBio, tvAvailability, tvEmail;
    private android.widget.RatingBar ratingBar;

    private CounselorController counselorController;
    private String counselorId;
    private String loadedCounselorName;
    private boolean isOwnProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_counselor_profile);

        counselorController = new CounselorController();
        counselorId = getIntent().getStringExtra(EXTRA_COUNSELOR_ID);
        String nameExtra = getIntent().getStringExtra(EXTRA_COUNSELOR_NAME);

        if (counselorId == null || counselorId.isEmpty()) {
            Toast.makeText(this, "Invalid counselor.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvName = findViewById(R.id.tvName);
        tvSpecialization = findViewById(R.id.tvSpecialization);
        tvBio = findViewById(R.id.tvBio);
        tvAvailability = findViewById(R.id.tvAvailability);
        tvEmail = findViewById(R.id.tvEmail);
        ratingBar = findViewById(R.id.ratingBar);
        btnViewSlots = findViewById(R.id.btnViewSlots);
        progressBar = findViewById(R.id.progressBar);
        cardOwnProfile = findViewById(R.id.cardOwnProfile);
        switchAccepting = findViewById(R.id.switchAccepting);
        btnSignOut = findViewById(R.id.btnSignOut);

        if (nameExtra != null && !nameExtra.isEmpty()) {
            tvName.setText(nameExtra);
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Counselor Profile");
        }

        String myUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        isOwnProfile = myUid != null && myUid.equals(counselorId);
        if (isOwnProfile) {
            cardOwnProfile.setVisibility(View.VISIBLE);
            btnSignOut.setVisibility(View.VISIBLE);
        }

        switchAccepting.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed()) return;
            FirebaseFirestore.getInstance().collection("counselors").document(counselorId)
                    .update("isAcceptingClients", isChecked);
        });

        loadProfile();

        btnViewSlots.setOnClickListener(v -> {
            Intent intent = new Intent(this, BookAppointmentActivity.class);
            intent.putExtra(BookAppointmentActivity.EXTRA_COUNSELOR_ID, counselorId);
            intent.putExtra(BookAppointmentActivity.EXTRA_COUNSELOR_NAME,
                    loadedCounselorName != null ? loadedCounselorName : tvName.getText().toString());
            startActivity(intent);
        });

        btnSignOut.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
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
            tvAvailability.setTextColor(Color.parseColor("#4CAF50"));
        } else {
            tvAvailability.setText("Not currently accepting new clients");
            tvAvailability.setTextColor(Color.parseColor("#F44336"));
            btnViewSlots.setEnabled(false);
        }

        if (isOwnProfile) {
            switchAccepting.setOnCheckedChangeListener(null);
            switchAccepting.setChecked(counselor.isAcceptingClients());
            switchAccepting.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (!buttonView.isPressed()) return;
                FirebaseFirestore.getInstance().collection("counselors").document(counselorId)
                        .update("isAcceptingClients", isChecked);
            });
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
