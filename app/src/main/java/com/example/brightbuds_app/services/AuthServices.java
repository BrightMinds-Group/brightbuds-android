package com.example.brightbuds_app.services;

import android.content.Context;
import android.util.Log;

import com.example.brightbuds_app.interfaces.DataCallbacks;
import com.example.brightbuds_app.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * AuthServices
 * ------------
 * Centralized authentication + Firestore user profile handler.
 * Phase 5 enhancements:
 *  - Enforces email verification
 *  - Stores verification flag in Firestore
 *  - Removes admin logic (parent-only)
 */
public class AuthServices {

    private static final String TAG = "AuthServices";
    private static final String USERS_COLLECTION = "users";

    private final FirebaseAuth auth;
    private final FirebaseFirestore db;
    private final Context context;

    public AuthServices(Context context) {
        this.context = context;
        this.auth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
    }

    // Register new Parent account + trigger email verification
    public void registerUser(String email, String password, String fullName,
                             DataCallbacks.GenericCallback callback) {

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser firebaseUser = result.getUser();
                    if (firebaseUser == null) {
                        callback.onFailure(new Exception("User creation failed — null FirebaseUser"));
                        return;
                    }

                    String uid = firebaseUser.getUid();
                    String userType = "parent";

                    // Prepare user document
                    Map<String, Object> data = new HashMap<>();
                    data.put("uid", uid);
                    data.put("fullName", fullName);
                    data.put("email", email);
                    data.put("type", userType);
                    data.put("createdAt", System.currentTimeMillis());
                    data.put("emailVerified", false);

                    // Save user profile
                    db.collection(USERS_COLLECTION)
                            .document(uid)
                            .set(data)
                            .addOnSuccessListener(aVoid -> {
                                Log.i(TAG, "✅ Firestore user profile created for " + email);

                                // Send verification link
                                firebaseUser.sendEmailVerification()
                                        .addOnSuccessListener(unused -> {
                                            Log.i(TAG, "📩 Verification email sent to " + email);
                                            callback.onSuccess(
                                                    "Account created! Please verify your email before logging in.");
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "⚠️ Failed to send verification email", e);
                                            callback.onFailure(e);
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "❌ Firestore user creation failed", e);
                                callback.onFailure(e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Firebase registration failed", e);
                    callback.onFailure(e);
                });
    }

    // Login user — enforce email verification before proceeding
    public void loginUser(String email, String password, DataCallbacks.GenericCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = auth.getCurrentUser();
                    if (firebaseUser == null) {
                        callback.onFailure(new Exception("Login failed — user is null."));
                        return;
                    }

                    if (!firebaseUser.isEmailVerified()) {
                        Log.w(TAG, "🚫 Unverified login attempt by " + email);
                        auth.signOut();
                        callback.onFailure(
                                new Exception("Please verify your email before logging in."));
                        return;
                    }

                    String uid = firebaseUser.getUid();

                    db.collection(USERS_COLLECTION)
                            .document(uid)
                            .get()
                            .addOnSuccessListener(doc -> {
                                if (!doc.exists()) {
                                    callback.onFailure(new Exception("User record not found in Firestore."));
                                    return;
                                }

                                String userType = doc.getString("type");
                                if (userType == null) userType = "parent";

                                // Update Firestore flag if newly verified
                                if (!Boolean.TRUE.equals(doc.getBoolean("emailVerified"))) {
                                    db.collection(USERS_COLLECTION)
                                            .document(uid)
                                            .update("emailVerified", true)
                                            .addOnSuccessListener(a -> Log.i(TAG, "Email verified flag updated"))
                                            .addOnFailureListener(e ->
                                                    Log.w(TAG, "⚠️ Failed to update verification flag", e));
                                }

                                Log.i(TAG, "✅ Login successful for " + email + " (" + userType + ")");
                                callback.onSuccess(userType);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "❌ Firestore lookup failed", e);
                                callback.onFailure(e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Login failed", e);
                    callback.onFailure(e);
                });
    }

    // Password reset
    public void resetPassword(String email, DataCallbacks.GenericCallback callback) {
        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(aVoid -> {
                    Log.i(TAG, "📩 Password reset link sent to " + email);
                    callback.onSuccess("Password reset link sent.");
                })
                .addOnFailureListener(callback::onFailure);
    }

    // Logout
    public void logout() {
        auth.signOut();
    }


    // Helper getters
    public boolean isLoggedIn() {
        return auth.getCurrentUser() != null;
    }

    public String getCurrentUserId() {
        FirebaseUser user = auth.getCurrentUser();
        return (user != null) ? user.getUid() : null;
    }

    public String getCurrentUserEmail() {
        FirebaseUser user = auth.getCurrentUser();
        return (user != null) ? user.getEmail() : null;
    }

    public String getCurrentUserName() {
        FirebaseUser user = auth.getCurrentUser();
        return (user != null && user.getDisplayName() != null)
                ? user.getDisplayName()
                : "Parent";
    }
}
