package com.example.brightbuds_app.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.brightbuds_app.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ParentProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final String TAG = "ParentProfileActivity";

    private TextView txtUserName, txtUserEmail, txtUserRole;
    private ImageView imgUserAvatar;
    private MaterialButton btnEditProfile, btnChangePassword, btnTermsConditions, btnLogout;
    private MaterialCardView cardPersonalInfo, cardAccountSettings;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private FirebaseUser currentUser;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_profile);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        loadUserData();
        setupClickListeners();
    }

    /** Initialize UI components */
    private void initializeViews() {
        txtUserName = findViewById(R.id.txtUserName);
        txtUserEmail = findViewById(R.id.txtUserEmail);
        txtUserRole = findViewById(R.id.txtUserRole);
        imgUserAvatar = findViewById(R.id.imgUserAvatar);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnTermsConditions = findViewById(R.id.btnTermsConditions);
        btnLogout = findViewById(R.id.btnLogout);
        cardPersonalInfo = findViewById(R.id.cardPersonalInfo);
        cardAccountSettings = findViewById(R.id.cardAccountSettings);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);
    }

    /** Load user data from Firebase Auth & Firestore */
    private void loadUserData() {
        txtUserName.setText(currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "Parent");
        txtUserEmail.setText(currentUser.getEmail());

        db.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String userType = documentSnapshot.getString("type");
                        txtUserRole.setText(userType != null && userType.equalsIgnoreCase("admin") ? "Administrator" : "Parent");

                        String avatarUrl = documentSnapshot.getString("avatarUrl");
                        if (avatarUrl != null && !avatarUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(avatarUrl)
                                    .placeholder(R.drawable.ic_child_placeholder)
                                    .error(R.drawable.ic_child_placeholder)
                                    .circleCrop()
                                    .into(imgUserAvatar);
                        }
                    } else {
                        txtUserRole.setText("Parent");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to load user data", e);
                    txtUserRole.setText("Parent");
                });

        // Also check for Auth profile image
        if (currentUser.getPhotoUrl() != null) {
            Glide.with(this)
                    .load(currentUser.getPhotoUrl())
                    .placeholder(R.drawable.ic_child_placeholder)
                    .error(R.drawable.ic_child_placeholder)
                    .circleCrop()
                    .into(imgUserAvatar);
        }
    }

    /** Setup button listeners */
    private void setupClickListeners() {
        btnEditProfile.setOnClickListener(v -> showEditProfileDialog());
        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        // Terms and Conditions button
        btnTermsConditions.setOnClickListener(v -> {
            Intent intent = new Intent(this, TermsConditionsActivity.class);
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> {
            auth.signOut();
            Intent intent = new Intent(this, LandingActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        imgUserAvatar.setOnClickListener(v -> changeProfilePicture());
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    /** Edit profile details */
    private void showEditProfileDialog() {
        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(this)
                .setTitle("Edit Profile");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_profile, null);
        TextInputEditText etDisplayName = dialogView.findViewById(R.id.etDisplayName);
        TextInputEditText etEmail = dialogView.findViewById(R.id.etEmail);

        etDisplayName.setText(currentUser.getDisplayName());
        etEmail.setText(currentUser.getEmail());
        dialogBuilder.setView(dialogView);

        dialogBuilder.setPositiveButton("Save", (dialog, which) -> {
            String newDisplayName = etDisplayName.getText().toString().trim();
            String newEmail = etEmail.getText().toString().trim();

            if (TextUtils.isEmpty(newDisplayName) || TextUtils.isEmpty(newEmail)) {
                Toast.makeText(this, "Please complete all fields", Toast.LENGTH_SHORT).show();
                return;
            }
            updateProfile(newDisplayName, newEmail);
        });

        dialogBuilder.setNegativeButton("Cancel", null);
        dialogBuilder.show();
    }

    /** Update Firebase profile */
    private void updateProfile(String displayName, String email) {
        progressDialog.setMessage("Updating profile...");
        progressDialog.show();

        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build();

        currentUser.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!email.equals(currentUser.getEmail())) {
                            currentUser.updateEmail(email)
                                    .addOnCompleteListener(emailTask -> {
                                        progressDialog.dismiss();
                                        if (emailTask.isSuccessful()) {
                                            updateFirestoreUserData(displayName, email);
                                            loadUserData();
                                            Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(this, "Email update failed", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            progressDialog.dismiss();
                            updateFirestoreUserData(displayName, email);
                            loadUserData();
                            Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Failed to update: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /** Update user Firestore document */
    private void updateFirestoreUserData(String displayName, String email) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", displayName);
        updates.put("email", email);

        db.collection("users").document(currentUser.getUid()).update(updates)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "✅ Firestore user data updated"))
                .addOnFailureListener(e -> Log.e(TAG, "❌ Firestore update failed", e));
    }

    /** Change password securely */
    private void showChangePasswordDialog() {
        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(this)
                .setTitle("Change Password");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        TextInputEditText etCurrentPassword = dialogView.findViewById(R.id.etCurrentPassword);
        TextInputEditText etNewPassword = dialogView.findViewById(R.id.etNewPassword);
        TextInputEditText etConfirmPassword = dialogView.findViewById(R.id.etConfirmPassword);

        dialogBuilder.setView(dialogView);
        dialogBuilder.setPositiveButton("Change", (dialog, which) -> {
            String current = etCurrentPassword.getText().toString().trim();
            String newPass = etNewPassword.getText().toString().trim();
            String confirm = etConfirmPassword.getText().toString().trim();

            if (TextUtils.isEmpty(current) || TextUtils.isEmpty(newPass) || TextUtils.isEmpty(confirm)) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPass.equals(confirm)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newPass.length() < 6) {
                Toast.makeText(this, "Password too short", Toast.LENGTH_SHORT).show();
                return;
            }

            changePassword(current, newPass);
        });

        dialogBuilder.setNegativeButton("Cancel", null);
        dialogBuilder.show();
    }

    /** Update password */
    private void changePassword(String currentPassword, String newPassword) {
        progressDialog.setMessage("Changing password...");
        progressDialog.show();

        AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), currentPassword);
        currentUser.reauthenticate(credential)
                .addOnSuccessListener(aVoid -> currentUser.updatePassword(newPassword)
                        .addOnCompleteListener(task -> {
                            progressDialog.dismiss();
                            if (task.isSuccessful()) {
                                Toast.makeText(this, "Password changed successfully!", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "Failed to change password", Toast.LENGTH_SHORT).show();
                            }
                        }))
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Incorrect current password", Toast.LENGTH_SHORT).show();
                });
    }

    /** Upload a new profile picture */
    private void changeProfilePicture() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Profile Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            uploadProfilePicture(data.getData());
        }
    }

    /** Upload profile picture to Firebase Storage */
    private void uploadProfilePicture(Uri imageUri) {
        progressDialog.setMessage("Uploading profile picture...");
        progressDialog.show();

        StorageReference ref = storage.getReference()
                .child("profile_pictures/" + currentUser.getUid() + "_" + UUID.randomUUID());

        ref.putFile(imageUri)
                .addOnSuccessListener(task -> ref.getDownloadUrl().addOnSuccessListener(uri -> {
                    currentUser.updateProfile(new UserProfileChangeRequest.Builder()
                                    .setPhotoUri(uri)
                                    .build())
                            .addOnSuccessListener(aVoid -> {
                                db.collection("users").document(currentUser.getUid())
                                        .update("avatarUrl", uri.toString());
                                Glide.with(this)
                                        .load(uri)
                                        .placeholder(R.drawable.ic_child_placeholder)
                                        .circleCrop()
                                        .into(imgUserAvatar);
                                progressDialog.dismiss();
                                Toast.makeText(this, "Profile picture updated!", Toast.LENGTH_SHORT).show();
                            });
                }))
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}
