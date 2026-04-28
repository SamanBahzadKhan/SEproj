package com.fridge.caps.views.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.fridge.caps.R;
import com.fridge.caps.controllers.JournalController;
import com.fridge.caps.models.JournalEntry;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Create or edit a single journal entry (title, mood, body).
 */
public class JournalEditActivity extends AppCompatActivity {

    public static final String EXTRA_ENTRY_ID   = "journal_entry_id";
    public static final String EXTRA_TITLE      = "journal_title";
    public static final String EXTRA_BODY       = "journal_body";
    public static final String EXTRA_MOOD       = "journal_mood";

    private EditText etTitle;
    private EditText etBody;
    private TextView chipHappy;
    private TextView chipNeutral;
    private TextView chipSad;
    private TextView tvScreenTitle;
    private MaterialButton btnSave;

    private String entryId;
    private String selectedMood = JournalEntry.MOOD_NEUTRAL;

    private JournalController journalController;

    public static Intent intentForEdit(Context context, JournalEntry entry) {
        Intent i = new Intent(context, JournalEditActivity.class);
        i.putExtra(EXTRA_ENTRY_ID, entry.getId());
        i.putExtra(EXTRA_TITLE, entry.getTitle());
        i.putExtra(EXTRA_BODY, entry.getBody());
        i.putExtra(EXTRA_MOOD, entry.getMood());
        return i;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_journal_edit);

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) {
            Toast.makeText(this, "Please sign in.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        journalController = new JournalController();

        etTitle = findViewById(R.id.etJournalTitle);
        etBody = findViewById(R.id.etJournalBody);
        chipHappy = findViewById(R.id.chipMoodHappy);
        chipNeutral = findViewById(R.id.chipMoodNeutral);
        chipSad = findViewById(R.id.chipMoodSad);
        tvScreenTitle = findViewById(R.id.tvEditScreenTitle);
        btnSave = findViewById(R.id.btnSaveJournal);

        entryId = getIntent().getStringExtra(EXTRA_ENTRY_ID);
        boolean isEdit = entryId != null && !entryId.isEmpty();
        tvScreenTitle.setText(isEdit ? "Edit entry" : "New entry");

        if (isEdit) {
            etTitle.setText(getIntent().getStringExtra(EXTRA_TITLE));
            etBody.setText(getIntent().getStringExtra(EXTRA_BODY));
            selectedMood = getIntent().getStringExtra(EXTRA_MOOD);
            if (selectedMood == null || selectedMood.isEmpty()) {
                selectedMood = JournalEntry.MOOD_NEUTRAL;
            }
        }

        findViewById(R.id.btnEditBack).setOnClickListener(v -> finish());

        chipHappy.setOnClickListener(v -> setMood(JournalEntry.MOOD_HAPPY));
        chipNeutral.setOnClickListener(v -> setMood(JournalEntry.MOOD_NEUTRAL));
        chipSad.setOnClickListener(v -> setMood(JournalEntry.MOOD_SAD));
        setMood(selectedMood);

        btnSave.setOnClickListener(v -> save(uid));
    }

    private void setMood(String mood) {
        selectedMood = mood != null ? mood : JournalEntry.MOOD_NEUTRAL;
        int on = R.drawable.bg_journal_mood_selected;
        int off = R.drawable.bg_brut_tag;
        chipHappy.setBackgroundResource(JournalEntry.MOOD_HAPPY.equals(selectedMood) ? on : off);
        chipNeutral.setBackgroundResource(JournalEntry.MOOD_NEUTRAL.equals(selectedMood) ? on : off);
        chipSad.setBackgroundResource(JournalEntry.MOOD_SAD.equals(selectedMood) ? on : off);
    }

    private void save(String uid) {
        String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
        String body = etBody.getText() != null ? etBody.getText().toString().trim() : "";
        if (title.isEmpty()) {
            Toast.makeText(this, "Please add a title.", Toast.LENGTH_SHORT).show();
            return;
        }
        btnSave.setEnabled(false);
        journalController.saveEntry(uid, entryId, title, body, selectedMood,
                new JournalController.JournalVoidCallback() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(JournalEditActivity.this, "Saved.", Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onFailure(String message) {
                        btnSave.setEnabled(true);
                        Toast.makeText(JournalEditActivity.this,
                                message != null ? message : "Save failed.",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}
