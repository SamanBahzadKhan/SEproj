package com.fridge.caps.views.activities;


/**
 * Purpose: Handles screen flow, UI state coordination, and user interactions.
 * Depends on: Android UI toolkit, app controllers/viewmodels, and navigation intents.
 * Notes: Focuses on presentation logic while delegating business rules to controllers.
 */
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.fridge.caps.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class EditProfileActivity extends AppCompatActivity {

    private static final String TAG = "EditProfile";

    private static final int PERMISSION_REQUEST = 1002;

    private EditText etName, etPhone, etDepartment, etYear;
    private CircleImageView avatarImage;
    private ProgressBar progressBar;
    private ProgressBar uploadProgressBar;

    private String currentUid;
    private String newProfilePictureUrl;

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() != RESULT_OK || result.getData() == null) return;
                Uri uri = result.getData().getData();
                if (uri == null) return;
                Glide.with(this).load(uri).circleCrop().into(avatarImage);
                uploadImageToStorage(uri);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        etDepartment = findViewById(R.id.etDepartment);
        etYear = findViewById(R.id.etYear);
        avatarImage = findViewById(R.id.avatarImage);
        progressBar = findViewById(R.id.progressBar);
        uploadProgressBar = findViewById(R.id.uploadProgressBar);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSaveToolbar).setOnClickListener(v -> saveProfile());

        currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (currentUid == null) {
            Toast.makeText(this, "Not logged in.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        FirebaseFirestore.getInstance().collection("students").document(currentUid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    etName.setText(doc.getString("name"));
                    etPhone.setText(doc.getString("phone"));
                    etDepartment.setText(doc.getString("department"));
                    etYear.setText(doc.getString("yearOfStudy"));
                    String picUrl = doc.getString("profilePictureUrl");
                    if (picUrl != null && !picUrl.isEmpty()) {
                        Glide.with(this).load(picUrl).circleCrop().into(avatarImage);
                    }
                });

        avatarImage.setOnClickListener(v -> openImagePicker());
        findViewById(R.id.cameraOverlay).setOnClickListener(v -> openImagePicker());
    }

    private void openImagePicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED) {
                launchPicker();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES}, PERMISSION_REQUEST);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                launchPicker();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST);
            }
        }
    }

    private void launchPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        pickImageLauncher.launch(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            launchPicker();
        } else if (requestCode == PERMISSION_REQUEST) {
            Toast.makeText(this, "Permission needed to select image", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadImageToStorage(Uri uri) {
        uploadProgressBar.setVisibility(android.view.View.VISIBLE);
        StorageReference ref = FirebaseStorage.getInstance().getReference()
                .child("profile_pictures/" + currentUid + ".jpg");
        ref.putFile(uri)
                .addOnSuccessListener(task -> ref.getDownloadUrl()
                        .addOnSuccessListener(downloadUri -> {
                            newProfilePictureUrl = downloadUri.toString();
                            uploadProgressBar.setVisibility(android.view.View.GONE);
                            Toast.makeText(this, "Photo ready — save to confirm", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            uploadProgressBar.setVisibility(android.view.View.GONE);
                            Log.e(TAG, "getDownloadUrl failed: " + e.getMessage());
                            Toast.makeText(this, "Failed to get image URL", Toast.LENGTH_SHORT).show();
                        }))
                .addOnFailureListener(e -> {
                    uploadProgressBar.setVisibility(android.view.View.GONE);
                    Log.e(TAG, "putFile failed: " + e.getMessage());
                    Toast.makeText(this,
                            "Upload failed: " + (e.getMessage() != null ? e.getMessage() : ""),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void saveProfile() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", etName.getText().toString().trim());
        updates.put("phone", etPhone.getText().toString().trim());
        updates.put("department", etDepartment.getText().toString().trim());
        updates.put("yearOfStudy", etYear.getText().toString().trim());
        if (newProfilePictureUrl != null) {
            updates.put("profilePictureUrl", newProfilePictureUrl);
        }

        progressBar.setVisibility(android.view.View.VISIBLE);
        FirebaseFirestore.getInstance().collection("students").document(currentUid)
                .update(updates)
                .addOnSuccessListener(v -> {
                    progressBar.setVisibility(android.view.View.GONE);
                    Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(android.view.View.GONE);
                    Toast.makeText(this,
                            "Update failed: " + (e.getMessage() != null ? e.getMessage() : ""),
                            Toast.LENGTH_SHORT).show();
                });
    }
}
