package com.fridge.caps.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fridge.caps.R;
import com.fridge.caps.controllers.JournalController;
import com.fridge.caps.models.JournalEntry;
import com.fridge.caps.utils.JournalStatsHelper;
import com.fridge.caps.views.adapters.JournalEntryCardAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

/**
 * Student journal hub: stats, new entry, and recent entry cards (neo-brutalist UI).
 */
public class MyJournalActivity extends AppCompatActivity implements JournalEntryCardAdapter.Listener {

    private TextView tvStatTotal;
    private TextView tvStatStreak;
    private TextView tvStatWeek;
    private TextView tvJournalEmpty;
    private RecyclerView rvJournalEntries;

    private final List<JournalEntry> entries = new ArrayList<>();
    private JournalEntryCardAdapter adapter;
    private JournalController journalController;
    private ListenerRegistration journalListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_journal);

        String uid = currentUid();
        if (uid == null) {
            Toast.makeText(this, "Please sign in.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        journalController = new JournalController();

        tvStatTotal = findViewById(R.id.tvStatTotal);
        tvStatStreak = findViewById(R.id.tvStatStreak);
        tvStatWeek = findViewById(R.id.tvStatWeek);
        tvJournalEmpty = findViewById(R.id.tvJournalEmpty);
        rvJournalEntries = findViewById(R.id.rvJournalEntries);

        findViewById(R.id.btnJournalBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnNewJournalEntry).setOnClickListener(v ->
                startActivity(new Intent(this, JournalEditActivity.class)));

        rvJournalEntries.setLayoutManager(new LinearLayoutManager(this));
        adapter = new JournalEntryCardAdapter(entries, this);
        rvJournalEntries.setAdapter(adapter);
        rvJournalEntries.setNestedScrollingEnabled(false);

        journalListener = journalController.listenEntries(uid, new JournalController.JournalListCallback() {
            @Override
            public void onSuccess(List<JournalEntry> list) {
                entries.clear();
                entries.addAll(list);
                adapter.notifyDataSetChanged();
                bindStats();
                boolean empty = entries.isEmpty();
                tvJournalEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                rvJournalEntries.setVisibility(empty ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onFailure(String message) {
                Toast.makeText(MyJournalActivity.this,
                        message != null ? message : "Could not load journal.",
                        Toast.LENGTH_LONG).show();
                tvJournalEmpty.setVisibility(View.VISIBLE);
                rvJournalEntries.setVisibility(View.GONE);
            }
        });
    }

    private void bindStats() {
        tvStatTotal.setText(String.valueOf(JournalStatsHelper.totalEntries(entries)));
        tvStatStreak.setText(String.valueOf(JournalStatsHelper.dayStreak(entries)));
        tvStatWeek.setText(String.valueOf(JournalStatsHelper.entriesThisWeek(entries)));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (journalListener != null) {
            journalListener.remove();
        }
    }

    private static String currentUid() {
        return FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
    }

    @Override
    public void onEdit(JournalEntry entry) {
        startActivity(JournalEditActivity.intentForEdit(this, entry));
    }

    @Override
    public void onOpen(JournalEntry entry) {
        onEdit(entry);
    }
}
