package com.fridge.caps.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
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

/**
 * CounselorListActivity — browse counsellors.
 */
public class CounselorListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar  progressBar;
    private TextView     tvEmpty;
    private EditText     etSearch;

    private CounselorController counselorController;
    private List<Counselor> allCounselors = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_counselor_list);

        counselorController = new CounselorController();

        recyclerView = findViewById(R.id.recyclerViewCounselors);
        progressBar  = findViewById(R.id.progressBar);
        tvEmpty      = findViewById(R.id.tvEmpty);
        etSearch     = findViewById(R.id.etSearch);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Counsellors");
        }

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilter(s != null ? s.toString() : "");
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        loadCounselors();
    }

    private void loadCounselors() {
        progressBar.setVisibility(View.VISIBLE);

        counselorController.getAllCounselors(new CounselorController.CounselorListCallback() {
            @Override
            public void onSuccess(List<Counselor> counselors) {
                progressBar.setVisibility(View.GONE);
                allCounselors = counselors;
                applyFilter(etSearch.getText().toString());
            }

            @Override
            public void onFailure(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(CounselorListActivity.this,
                        "Failed to load counselors: " + errorMessage,
                        Toast.LENGTH_SHORT).show();
            }
        });
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
                Intent i = new Intent(CounselorListActivity.this,
                        CounselorProfileActivity.class);
                i.putExtra(CounselorProfileActivity.EXTRA_COUNSELOR_ID,
                        counselor.getUserId());
                if (counselor.getName() != null) {
                    i.putExtra(CounselorProfileActivity.EXTRA_COUNSELOR_NAME, counselor.getName());
                }
                startActivity(i);
            }));
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
