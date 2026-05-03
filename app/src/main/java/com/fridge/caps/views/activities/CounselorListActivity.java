package com.fridge.caps.views.activities;


/**
 * Purpose: Handles screen flow, UI state coordination, and user interactions.
 * Depends on: Android UI toolkit, app controllers/viewmodels, and navigation intents.
 * Notes: Focuses on presentation logic while delegating business rules to controllers.
 */
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fridge.caps.R;
import com.fridge.caps.controllers.CounselorController;
import com.fridge.caps.models.Counselor;
import com.fridge.caps.views.adapters.CounselorAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class CounselorListActivity extends AppCompatActivity {

    public static final String EXTRA_ADMIN_COUNSELOR_LIST = "admin_counselor_list";

    private RecyclerView recyclerView;
    private ProgressBar  progressBar;
    private TextView     tvEmpty;
    private EditText     etSearch;
    private TextView     tvTitle;

    private CounselorController counselorController;
    private List<Counselor> allCounselors = new ArrayList<>();
    private boolean adminListMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_counselor_list);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        adminListMode = getIntent().getBooleanExtra(EXTRA_ADMIN_COUNSELOR_LIST, false);

        counselorController = new CounselorController();

        recyclerView = findViewById(R.id.recyclerViewCounselors);
        progressBar  = findViewById(R.id.progressBar);
        tvEmpty      = findViewById(R.id.tvEmpty);
        etSearch     = findViewById(R.id.etSearch);
        tvTitle      = findViewById(R.id.tvCounselorListTitle);
        ImageButton back = findViewById(R.id.topBarBack);
        back.setOnClickListener(v -> finish());

        tvTitle.setText(adminListMode ? "All Counsellors" : "Find a Counsellor");
        if (!adminListMode) {
            etSearch.setHint("Search counsellors...");
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilter(s != null ? s.toString() : "");
            }
            @Override public void afterTextChanged(Editable s) { }
        });

        loadCounselors();
    }

    private void loadCounselors() {
        progressBar.setVisibility(View.VISIBLE);

        CounselorController.CounselorListCallback cb = new CounselorController.CounselorListCallback() {
            @Override
            public void onSuccess(List<Counselor> counselors) {
                progressBar.setVisibility(View.GONE);
                allCounselors = counselors != null ? counselors : new ArrayList<>();
                applyFilter(etSearch.getText().toString());
            }

            @Override
            public void onFailure(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(CounselorListActivity.this,
                        "Failed to load counselors: " + errorMessage,
                        Toast.LENGTH_SHORT).show();
            }
        };

        if (adminListMode) {
            counselorController.getAllCounselorsForAdmin(cb);
        } else {
            counselorController.getAllCounselors(cb);
        }
    }

    private void applyFilter(String query) {
        String q = query.trim().toLowerCase(Locale.getDefault());
        List<Counselor> filtered = allCounselors.stream()
                .filter(c -> {
                    if (q.isEmpty()) return true;
                    String name = c.getName() != null ? c.getName().toLowerCase(Locale.getDefault()) : "";
                    String spec = c.getSpecialization() != null
                            ? c.getSpecialization().toLowerCase(Locale.getDefault()) : "";
                    return name.contains(q) || spec.contains(q);
                })
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            recyclerView.setAdapter(null);
        } else {
            tvEmpty.setVisibility(View.GONE);
            recyclerView.setAdapter(new CounselorAdapter(filtered, counselor -> {
                Intent i = new Intent(CounselorListActivity.this, CounselorProfileActivity.class);
                i.putExtra(CounselorProfileActivity.EXTRA_COUNSELOR_ID, counselor.getUserId());
                i.putExtra("counselorId", counselor.getUserId());
                if (counselor.getName() != null) {
                    i.putExtra(CounselorProfileActivity.EXTRA_COUNSELOR_NAME, counselor.getName());
                }
                startActivity(i);
            }));
        }
    }
}
