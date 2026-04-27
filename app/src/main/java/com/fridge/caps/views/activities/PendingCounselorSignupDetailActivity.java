package com.fridge.caps.views.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.fridge.caps.R;
import com.fridge.caps.controllers.CounselorSignupController;
import com.fridge.caps.models.PendingCounselorSignup;

public class PendingCounselorSignupDetailActivity extends AppCompatActivity {
    public static final String EXTRA_SIGNUP_ID = "signupId";
    private CounselorSignupController controller;
    private PendingCounselorSignup loaded;
    private View progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_signup_detail);
        controller = new CounselorSignupController();
        progress = findViewById(R.id.progressBar);

        String signupId = getIntent().getStringExtra(EXTRA_SIGNUP_ID);
        if (signupId == null || signupId.isEmpty()) {
            Toast.makeText(this, "Missing signup.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnApprove).setOnClickListener(v -> approve());
        findViewById(R.id.btnReject).setOnClickListener(v -> reject());
        load(signupId);
    }

    private void load(String id) {
        progress.setVisibility(View.VISIBLE);
        controller.getSignupById(id, new CounselorSignupController.SignupCallback() {
            @Override
            public void onSuccess(PendingCounselorSignup signup) {
                progress.setVisibility(View.GONE);
                loaded = signup;
                ((TextView) findViewById(R.id.tvName)).setText(v(signup.getName()));
                ((TextView) findViewById(R.id.tvEmail)).setText(v(signup.getEmail()));
                ((TextView) findViewById(R.id.tvSpec)).setText(v(signup.getSpecialization()));
                ((TextView) findViewById(R.id.tvDept)).setText(v(signup.getDepartment()));
                ((TextView) findViewById(R.id.tvPhone)).setText(v(signup.getPhone()));
                ((TextView) findViewById(R.id.tvBio)).setText(v(signup.getBio()));
            }

            @Override
            public void onFailure(String error) {
                progress.setVisibility(View.GONE);
                Toast.makeText(PendingCounselorSignupDetailActivity.this, error, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void approve() {
        if (loaded == null) return;
        progress.setVisibility(View.VISIBLE);
        controller.approveSignup(loaded, new CounselorSignupController.ActionCallback() {
            @Override public void onSuccess() {
                progress.setVisibility(View.GONE);
                Toast.makeText(PendingCounselorSignupDetailActivity.this, "Approved.", Toast.LENGTH_SHORT).show();
                finish();
            }
            @Override public void onFailure(String error) {
                progress.setVisibility(View.GONE);
                Toast.makeText(PendingCounselorSignupDetailActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void reject() {
        if (loaded == null) return;
        progress.setVisibility(View.VISIBLE);
        controller.rejectSignup(loaded, new CounselorSignupController.ActionCallback() {
            @Override public void onSuccess() {
                progress.setVisibility(View.GONE);
                Toast.makeText(PendingCounselorSignupDetailActivity.this, "Rejected.", Toast.LENGTH_SHORT).show();
                finish();
            }
            @Override public void onFailure(String error) {
                progress.setVisibility(View.GONE);
                Toast.makeText(PendingCounselorSignupDetailActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String v(String s) { return s != null && !s.isEmpty() ? s : "—"; }
}
