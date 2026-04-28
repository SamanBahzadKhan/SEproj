package com.fridge.caps.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

/**
 * Single student journal entry stored under {@code students/{uid}/journalEntries}.
 */
public class JournalEntry {

    public static final String MOOD_HAPPY   = "happy";
    public static final String MOOD_NEUTRAL = "neutral";
    public static final String MOOD_SAD     = "sad";

    private String id;
    private String title;
    private String body;
    private String mood;
    private long createdAtMillis;

    public JournalEntry() {}

    public JournalEntry(String id, String title, String body, String mood, long createdAtMillis) {
        this.id = id;
        this.title = title;
        this.body = body;
        this.mood = mood != null ? mood : MOOD_NEUTRAL;
        this.createdAtMillis = createdAtMillis;
    }

    public static JournalEntry fromSnapshot(DocumentSnapshot snap) {
        JournalEntry e = new JournalEntry();
        e.id = snap.getId();
        e.title = snap.getString("title");
        e.body = snap.getString("body");
        e.mood = snap.getString("mood");
        if (e.mood == null || e.mood.isEmpty()) {
            e.mood = MOOD_NEUTRAL;
        }
        Object ts = snap.get("createdAt");
        if (ts instanceof Timestamp) {
            e.createdAtMillis = ((Timestamp) ts).toDate().getTime();
        } else if (ts instanceof Long) {
            e.createdAtMillis = (Long) ts;
        } else if (ts instanceof Number) {
            e.createdAtMillis = ((Number) ts).longValue();
        } else {
            e.createdAtMillis = System.currentTimeMillis();
        }
        return e;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title != null ? title : "";
    }

    public String getBody() {
        return body != null ? body : "";
    }

    public String getMood() {
        return mood != null ? mood : MOOD_NEUTRAL;
    }

    public long getCreatedAtMillis() {
        return createdAtMillis;
    }
}
