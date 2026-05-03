package com.fridge.caps.views.activities;


/**
 * Purpose: Handles screen flow, UI state coordination, and user interactions.
 * Depends on: Android UI toolkit, app controllers/viewmodels, and navigation intents.
 * Notes: Focuses on presentation logic while delegating business rules to controllers.
 */
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.fridge.caps.AppConfig;
import com.fridge.caps.R;
import com.google.firebase.FirebaseApp;
import com.google.firebase.functions.FirebaseFunctions;

import java.util.HashMap;
import java.util.Map;

public class AddCounselorActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPassword, etSpec, etDept, etPhone;
    private SwitchCompat swAccepting;
    private Button btnCreate;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_counselor);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etSpec = findViewById(R.id.etSpecialization);
        etDept = findViewById(R.id.etDepartment);
        etPhone = findViewById(R.id.etPhone);
        swAccepting = findViewById(R.id.switchAcceptingClients);
        btnCreate = findViewById(R.id.btnCreateCounselor);
        progressBar = findViewById(R.id.progressBar);

        btnCreate.setOnClickListener(v -> submit());
    }

    private void submit() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();
        String spec = etSpec.getText().toString().trim();
        String dept = etDept.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Name, email, and password are required.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (pass.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("email", email);
        data.put("password", pass);
        data.put("specialization", spec);
        data.put("department", dept);
        data.put("phone", phone);
        data.put("acceptingClients", swAccepting.isChecked());

        FirebaseFunctions.getInstance(FirebaseApp.getInstance(), AppConfig.FIREBASE_FUNCTIONS_REGION)
                .getHttpsCallable("createCounsellorAccount")
                .call(data)
                .addOnSuccessListener(result -> {
                    setLoading(false);
                    Toast.makeText(this, "Counsellor account created", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    String msg = e.getMessage() != null ? e.getMessage() : "Failed to create account.";
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                });
    }

    private void setLoading(boolean on) {
        progressBar.setVisibility(on ? View.VISIBLE : View.GONE);
        btnCreate.setEnabled(!on);
    }
}
