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
import com.fridge.caps.views.adapters.JournalListRowAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple chronological list of journal entry titles and timestamps (student dashboard pill).
 */
public class JournalEntriesListActivity extends AppCompatActivity implements JournalListRowAdapter.Listener {

    private TextView tvListEmpty;
    private RecyclerView rvJournalList;
    private final List<JournalEntry> entries = new ArrayList<>();
    private JournalListRowAdapter adapter;
    private JournalController journalController;
    private ListenerRegistration journalListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_journal_entries_list);

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) {
            Toast.makeText(this, "Please sign in.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        journalController = new JournalController();
        tvListEmpty = findViewById(R.id.tvListEmpty);
        rvJournalList = findViewById(R.id.rvJournalList);

        findViewById(R.id.btnJournalListBack).setOnClickListener(v -> finish());

        rvJournalList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new JournalListRowAdapter(entries, this);
        rvJournalList.setAdapter(adapter);

        journalListener = journalController.listenEntries(uid, new JournalController.JournalListCallback() {
            @Override
            public void onSuccess(List<JournalEntry> list) {
                entries.clear();
                entries.addAll(list);
                adapter.notifyDataSetChanged();
                boolean empty = entries.isEmpty();
                tvListEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                rvJournalList.setVisibility(empty ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onFailure(String message) {
                Toast.makeText(JournalEntriesListActivity.this,
                        message != null ? message : "Could not load journal.",
                        Toast.LENGTH_LONG).show();
                tvListEmpty.setVisibility(View.VISIBLE);
                rvJournalList.setVisibility(View.GONE);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (journalListener != null) {
            journalListener.remove();
        }
    }

    @Override
    public void onRowClick(JournalEntry entry) {
        startActivity(JournalEditActivity.intentForEdit(this, entry));
    }
}
