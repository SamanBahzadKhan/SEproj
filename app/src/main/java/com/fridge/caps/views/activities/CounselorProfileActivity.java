package com.fridge.caps.views.activities;

/**
 * CounselorProfileActivity.java
 * Displays detailed counselor profile with ratings, reviews, and booking options.
 * Shows counselor specialization, bio, feedback history, and allows students to book appointments.
 * View in the MVC pattern.
 */
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fridge.caps.R;
import com.fridge.caps.models.Counselor;
import com.fridge.caps.models.FeedbackItem;
import com.fridge.caps.utils.RatingDisplayHelper;
import com.fridge.caps.views.adapters.FeedbackReviewAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Counsellor profile for students/admins; own profile adds accepting switch + sign out.
 */
public class CounselorProfileActivity extends AppCompatActivity {

    private static final String TAG = "CounselorProfile";

    public static final String EXTRA_COUNSELOR_ID   = "counselor_id";
    public static final String EXTRA_COUNSELOR_NAME = "counselor_name";

    private View btnEditProfile;
    private View barBookAppointmentBottom;
    private com.google.android.material.button.MaterialButton btnBookAppointment;
    private View btnSignOut;
    private SwitchCompat switchAccepting;
    private View cardOwnProfile;
    private android.widget.ProgressBar progressBar;
    private TextView tvName, tvSpecialization, tvBio, tvAvailability, tvEmail;
    private TextView tvRatingValue, tvRatingCount;
    private TextView tvNoReviews;
    private TextView tvAvatarInitials;
    private RecyclerView rvReviews;

    private ImageView[] starAvg;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ListenerRegistration counselorListener;

    private FeedbackReviewAdapter reviewsAdapter;

    private String counselorId;
    private String loadedCounselorName;
    private boolean isOwnProfile;
    private boolean counselorAcceptingClients = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_counselor_profile);

        counselorId = getIntent().getStringExtra(EXTRA_COUNSELOR_ID);
        if (counselorId == null || counselorId.isEmpty()) {
            counselorId = getIntent().getStringExtra("counselorId");
        }
        if (counselorId == null || counselorId.isEmpty()) {
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                counselorId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            }
        }
        String nameExtra = getIntent().getStringExtra(EXTRA_COUNSELOR_NAME);

        if (counselorId == null || counselorId.isEmpty()) {
            Toast.makeText(this, "Invalid counselor.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.d(TAG, "Loading profile for counselorId: " + counselorId);

        tvName = findViewById(R.id.tvName);
        tvSpecialization = findViewById(R.id.tvSpecialization);
        tvBio = findViewById(R.id.tvBio);
        tvAvailability = findViewById(R.id.tvAvailability);
        tvEmail = findViewById(R.id.tvEmail);
        tvRatingValue = findViewById(R.id.tvRatingValue);
        tvRatingCount = findViewById(R.id.tvRatingCount);
        tvNoReviews = findViewById(R.id.tvNoReviews);
        tvAvatarInitials = findViewById(R.id.tvAvatarInitials);
        rvReviews = findViewById(R.id.rvReviews);
        progressBar = findViewById(R.id.progressBar);
        cardOwnProfile = findViewById(R.id.cardOwnProfile);
        switchAccepting = findViewById(R.id.switchAccepting);
        btnSignOut = findViewById(R.id.btnSignOut);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        barBookAppointmentBottom = findViewById(R.id.barBookAppointmentBottom);
        btnBookAppointment = findViewById(R.id.btnBookAppointment);

        starAvg = new ImageView[]{
                findViewById(R.id.starAvg1),
                findViewById(R.id.starAvg2),
                findViewById(R.id.starAvg3),
                findViewById(R.id.starAvg4),
                findViewById(R.id.starAvg5),
        };

        rvReviews.setLayoutManager(new LinearLayoutManager(this));
        reviewsAdapter = new FeedbackReviewAdapter();
        rvReviews.setAdapter(reviewsAdapter);

        if (nameExtra != null && !nameExtra.isEmpty()) {
            tvName.setText(withDrPrefix(nameExtra));
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
            btnEditProfile.setVisibility(View.VISIBLE);
            btnEditProfile.setOnClickListener(v ->
                    startActivity(new Intent(this, EditCounselorProfileActivity.class)));
            if (barBookAppointmentBottom != null) {
                barBookAppointmentBottom.setVisibility(View.GONE);
            }
        } else {
            btnEditProfile.setVisibility(View.GONE);
            if (myUid != null && btnBookAppointment != null) {
                db.collection("students").document(myUid).get()
                        .addOnSuccessListener(studentDoc -> {
                            if (studentDoc.exists()) {
                                if (barBookAppointmentBottom != null) {
                                    barBookAppointmentBottom.setVisibility(View.VISIBLE);
                                }
                                btnBookAppointment.setVisibility(View.VISIBLE);
                                btnBookAppointment.setOnClickListener(v -> {
                                    Intent intent = new Intent(this, BookAppointmentActivity.class);
                                    intent.putExtra(BookAppointmentActivity.EXTRA_COUNSELOR_ID, counselorId);
                                    intent.putExtra(BookAppointmentActivity.EXTRA_COUNSELOR_NAME,
                                            loadedCounselorName != null
                                                    ? loadedCounselorName
                                                    : tvName.getText().toString());
                                    startActivity(intent);
                                });
                                refreshBookAppointmentEnabled();
                            }
                        });
            }
        }

        switchAccepting.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed()) return;
            db.collection("counselors").document(counselorId)
                    .update("isAcceptingClients", isChecked);
        });

        progressBar.setVisibility(View.VISIBLE);

        counselorListener = db.collection("counselors").document(counselorId)
                .addSnapshotListener((snap, err) -> {
                    progressBar.setVisibility(View.GONE);
                    if (err != null || snap == null || !snap.exists()) {
                        if (err != null) {
                            Toast.makeText(this, err.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                        return;
                    }
                    Counselor counselor = Counselor.fromDocument(snap);
                    applyCounselorDoc(snap, counselor);
                });

        btnSignOut.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void applyCounselorDoc(DocumentSnapshot snap, Counselor counselor) {
        if (counselor == null) return;
        loadedCounselorName = counselor.getName();
        tvName.setText(withDrPrefix(counselor.getName()));
        if (tvAvatarInitials != null) {
            tvAvatarInitials.setText(initialsOf(counselor.getName()));
        }
        tvSpecialization.setText(counselor.getSpecialization());
        tvBio.setText(counselor.getBio());
        tvEmail.setText(counselor.getEmail());

        double rating = 0.0;
        if (snap.contains("rating")) {
            Double d = snap.getDouble("rating");
            if (d != null) rating = d;
        } else {
            rating = counselor.getRating();
        }
        int count = counselor.getRatingCount();
        if (snap.contains("ratingCount")) {
            Long rc = snap.getLong("ratingCount");
            if (rc != null) count = rc.intValue();
        }

        tvRatingValue.setText(String.format(java.util.Locale.US, "%.1f", rating));
        tvRatingCount.setText("(" + count + " reviews)");
        RatingDisplayHelper.applyStarRating(starAvg, rating);

        if (counselor.isAcceptingClients()) {
            tvAvailability.setText("Currently accepting new clients");
            tvAvailability.setTextColor(Color.parseColor("#4CAF50"));
        } else {
            tvAvailability.setText("Not currently accepting new clients");
            tvAvailability.setTextColor(Color.parseColor("#F44336"));
        }

        if (isOwnProfile) {
            switchAccepting.setOnCheckedChangeListener(null);
            switchAccepting.setChecked(counselor.isAcceptingClients());
            switchAccepting.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (!buttonView.isPressed()) return;
                db.collection("counselors").document(counselorId)
                        .update("isAcceptingClients", isChecked);
            });
        }

        counselorAcceptingClients = counselor.isAcceptingClients();
        refreshBookAppointmentEnabled();
    }

    private void refreshBookAppointmentEnabled() {
        if (btnBookAppointment == null || btnBookAppointment.getVisibility() != View.VISIBLE) {
            return;
        }
        btnBookAppointment.setEnabled(counselorAcceptingClients);
        btnBookAppointment.setAlpha(counselorAcceptingClients ? 1f : 0.5f);
    }

    private void loadFeedbackReviews() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
        if (uid == null) {
            loadFeedbackReviewsForViewer(false);
            return;
        }
        db.collection("admins").document(uid).get()
                .addOnSuccessListener(adminDoc -> loadFeedbackReviewsForViewer(adminDoc.exists()))
                .addOnFailureListener(e -> loadFeedbackReviewsForViewer(false));
    }

    /**
     * Students and counsellors see reviewer as anonymous; admins (collection {@code admins}) see stored names.
     */
    private void loadFeedbackReviewsForViewer(boolean viewerIsAdmin) {
        db.collection("feedback")
                .whereEqualTo("counselorId", counselorId)
                .get()
                .addOnSuccessListener(snap -> {
                    List<QueryDocumentSnapshot> docs = new ArrayList<>();
                    for (QueryDocumentSnapshot d : snap) {
                        docs.add(d);
                    }
                    if (docs.isEmpty()) {
                        tvNoReviews.setText("No reviews yet");
                        tvNoReviews.setVisibility(View.VISIBLE);
                        rvReviews.setVisibility(View.GONE);
                        reviewsAdapter.updateData(new ArrayList<>());
                        return;
                    }

                    Collections.sort(docs, (a, b) -> {
                        Long tA = a.getLong("timestamp");
                        Long tB = b.getLong("timestamp");
                        if (tA == null) tA = 0L;
                        if (tB == null) tB = 0L;
                        return Long.compare(tB, tA);
                    });

                    String anonymous = getString(R.string.anonymous_feedback);
                    List<FeedbackItem> reviews = new ArrayList<>();
                    for (DocumentSnapshot doc : docs) {
                        String storedName = doc.getString("studentName");
                        String displayName;
                        if (viewerIsAdmin && storedName != null && !storedName.trim().isEmpty()) {
                            displayName = storedName.trim();
                        } else {
                            displayName = anonymous;
                        }
                        Long r = doc.getLong("rating");
                        int rating = r != null ? r.intValue() : 0;
                        String comment = doc.getString("comment");
                        long ts = 0L;
                        Object t = doc.get("timestamp");
                        if (t instanceof Long) {
                            ts = (Long) t;
                        }
                        reviews.add(new FeedbackItem(displayName, rating, comment, ts));
                    }

                    tvNoReviews.setVisibility(View.GONE);
                    rvReviews.setVisibility(View.VISIBLE);
                    reviewsAdapter.updateData(reviews);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load reviews: " + (e.getMessage() != null ? e.getMessage() : ""));
                    tvNoReviews.setText("Unable to load reviews.");
                    tvNoReviews.setVisibility(View.VISIBLE);
                    rvReviews.setVisibility(View.GONE);
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFeedbackReviews();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (counselorListener != null) {
            counselorListener.remove();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private String withDrPrefix(String name) {
        if (name == null) return "Dr.";
        String trimmed = name.trim();
        if (trimmed.toLowerCase(java.util.Locale.US).startsWith("dr.")) {
            return trimmed;
        }
        return "Dr. " + trimmed;
    }

    private String initialsOf(String name) {
        if (name == null || name.trim().isEmpty()) return "DR";
        String clean = name.trim().replaceFirst("(?i)^dr\\.\\s*", "").trim();
        if (clean.isEmpty()) return "DR";
        String[] parts = clean.split("\\s+");
        if (parts.length == 0) return "DR";
        if (parts.length == 1) {
            String only = parts[0];
            if (only.isEmpty()) return "DR";
            return only.substring(0, 1).toUpperCase(java.util.Locale.US);
        }
        String first = parts[0];
        String last = parts[parts.length - 1];
        if (first.isEmpty() || last.isEmpty()) return "DR";
        return first.substring(0, 1).toUpperCase(java.util.Locale.US)
                + last.substring(0, 1).toUpperCase(java.util.Locale.US);
    }
}
