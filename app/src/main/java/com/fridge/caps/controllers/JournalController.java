package com.fridge.caps.controllers;



/**
 * Purpose: Handles application business rules and data operations.
 * Depends on: Firebase Firestore/Auth models and app domain objects.
 * Notes: Coordinates validation and state changes used by app flows.
 */
/**
 * Purpose: Handles application business rules and data operations.
 * Depends on: Firebase Firestore/Auth models and app domain objects.
 * Notes: Coordinates validation and state changes used by app flows.
 */
import com.fridge.caps.models.JournalEntry;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JournalController {

    private static final String COL_STUDENTS = "students";
    private static final String SUB_JOURNAL  = "journalEntries";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface JournalListCallback {
        void onSuccess(List<JournalEntry> entries);

        void onFailure(String message);
    }

    
    public ListenerRegistration listenEntries(String studentId, JournalListCallback callback) {
        if (studentId == null || studentId.isEmpty()) {
            callback.onSuccess(new ArrayList<>());
            return null;
        }
        return db.collection(COL_STUDENTS).document(studentId).collection(SUB_JOURNAL)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        callback.onFailure(e.getMessage() != null ? e.getMessage() : "Journal load failed");
                        return;
                    }
                    if (snap == null) {
                        callback.onSuccess(new ArrayList<>());
                        return;
                    }
                    List<JournalEntry> out = new ArrayList<>();
                    snap.getDocuments().forEach(d -> out.add(JournalEntry.fromSnapshot(d)));
                    callback.onSuccess(out);
                });
    }

    public void saveEntry(String studentId, String entryId, String title, String body, String mood,
                          JournalVoidCallback callback) {
        if (studentId == null || studentId.isEmpty()) {
            if (callback != null) {
                callback.onFailure("Not signed in.");
            }
            return;
        }
        DocumentReference ref;
        boolean isNew = entryId == null || entryId.isEmpty();
        if (isNew) {
            ref = db.collection(COL_STUDENTS).document(studentId).collection(SUB_JOURNAL).document();
        } else {
            ref = db.collection(COL_STUDENTS).document(studentId).collection(SUB_JOURNAL).document(entryId);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("title", title != null ? title : "");
        data.put("body", body != null ? body : "");
        data.put("mood", mood != null ? mood : JournalEntry.MOOD_NEUTRAL);
        if (isNew) {
            data.put("createdAt", FieldValue.serverTimestamp());
        } else {
            data.put("updatedAt", FieldValue.serverTimestamp());
        }

        ref.set(data, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(v -> {
                    if (callback != null) {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(ex -> {
                    if (callback != null) {
                        callback.onFailure(ex.getMessage() != null ? ex.getMessage() : "Save failed");
                    }
                });
    }

    public void deleteEntry(String studentId, String entryId, JournalVoidCallback callback) {
        if (studentId == null || studentId.isEmpty() || entryId == null || entryId.isEmpty()) {
            if (callback != null) {
                callback.onFailure("Invalid entry.");
            }
            return;
        }
        db.collection(COL_STUDENTS).document(studentId).collection(SUB_JOURNAL).document(entryId)
                .delete()
                .addOnSuccessListener(v -> {
                    if (callback != null) {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(ex -> {
                    if (callback != null) {
                        callback.onFailure(ex.getMessage() != null ? ex.getMessage() : "Delete failed");
                    }
                });
    }

    public interface JournalVoidCallback {
        void onSuccess();

        void onFailure(String message);
    }
}
