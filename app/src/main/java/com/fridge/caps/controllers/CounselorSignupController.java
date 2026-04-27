package com.fridge.caps.controllers;

import com.fridge.caps.models.PendingCounselorSignup;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CounselorSignupController {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface SignupListCallback {
        void onSuccess(List<PendingCounselorSignup> signups);
        void onFailure(String error);
    }

    public interface ActionCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public interface SignupCallback {
        void onSuccess(PendingCounselorSignup signup);
        void onFailure(String error);
    }

    public ListenerRegistration listenPendingSignups(SignupListCallback callback) {
        return db.collection("pending_counselor_signups")
                .whereEqualTo("status", "pending")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) {
                        callback.onFailure(e != null ? e.getMessage() : "Failed to load signups.");
                        return;
                    }
                    List<PendingCounselorSignup> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        list.add(PendingCounselorSignup.fromDocument(doc));
                    }
                    callback.onSuccess(list);
                });
    }

    public void getSignupById(String signupId, SignupCallback callback) {
        db.collection("pending_counselor_signups").document(signupId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        callback.onFailure("Signup request not found.");
                        return;
                    }
                    callback.onSuccess(PendingCounselorSignup.fromDocument(doc));
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void approveSignup(PendingCounselorSignup signup, ActionCallback callback) {
        if (signup == null || signup.getUserId() == null || signup.getSignupId() == null) {
            callback.onFailure("Invalid signup request.");
            return;
        }
        Map<String, Object> counselor = new HashMap<>();
        counselor.put("userId", signup.getUserId());
        counselor.put("name", safe(signup.getName()));
        counselor.put("email", safe(signup.getEmail()));
        counselor.put("specialization", safe(signup.getSpecialization()));
        counselor.put("department", safe(signup.getDepartment()));
        counselor.put("phone", safe(signup.getPhone()));
        counselor.put("bio", safe(signup.getBio()));
        counselor.put("role", "COUNSELOR");
        counselor.put("rating", 0.0);
        counselor.put("isAcceptingClients", signup.getAcceptingClients() == null
                || Boolean.TRUE.equals(signup.getAcceptingClients()));
        counselor.put("isActive", true);
        counselor.put("isDeleted", false);
        counselor.put("createdAt", com.google.firebase.Timestamp.now());

        db.collection("counselors").document(signup.getUserId()).set(counselor)
                .addOnSuccessListener(v -> db.collection("pending_counselor_signups")
                        .document(signup.getSignupId()).update("status", "approved")
                        .addOnSuccessListener(done -> callback.onSuccess())
                        .addOnFailureListener(e -> callback.onFailure(e.getMessage())))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void rejectSignup(PendingCounselorSignup signup, ActionCallback callback) {
        if (signup == null || signup.getSignupId() == null) {
            callback.onFailure("Invalid signup request.");
            return;
        }
        db.collection("pending_counselor_signups")
                .document(signup.getSignupId())
                .update("status", "rejected")
                .addOnSuccessListener(v -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    private String safe(String s) { return s != null ? s : ""; }
}
