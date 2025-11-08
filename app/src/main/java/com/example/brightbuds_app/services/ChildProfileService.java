package com.example.brightbuds_app.services;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.brightbuds_app.interfaces.DataCallbacks;
import com.example.brightbuds_app.models.ChildProfile;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles CRUD operations for Child Profiles (Firestore + Encryption fallback).
 */
public class ChildProfileService {

    private static final String TAG = "ChildProfileService";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    /** Save child profile with MAXIMUM DEBUGGING */
    public void saveChildProfile(@NonNull ChildProfile newChild, @NonNull DataCallbacks.GenericCallback callback) {
        Log.d(TAG, "🎯 STARTING SAVE CHILD PROFILE PROCESS");

        try {
            // Check parentId FIRST
            String parentId = newChild.getParentId();
            Log.d(TAG, "🔍 STEP 1 - ParentId: " + parentId);

            if (parentId == null || parentId.isEmpty()) {
                Log.e(TAG, "❌ CRITICAL FAILURE: ParentId is NULL or EMPTY");
                callback.onFailure(new IllegalStateException("ParentId missing"));
                return;
            }

            // Check ALL child data
            Log.d(TAG, "🔍 STEP 2 - CHILD DATA AUDIT:");
            Log.d(TAG, "   - ParentId: " + newChild.getParentId());
            Log.d(TAG, "   - Name: " + newChild.getName());
            Log.d(TAG, "   - Age: " + newChild.getAge());
            Log.d(TAG, "   - Gender: " + newChild.getGender());
            Log.d(TAG, "   - LearningLevel: " + newChild.getLearningLevel());
            Log.d(TAG, "   - DisplayName: " + newChild.getDisplayName());
            Log.d(TAG, "   - Existing ChildId: " + newChild.getChildId());

            // Generate childId if missing
            String childId = newChild.getChildId();
            if (childId == null || childId.isEmpty()) {
                childId = db.collection("child_profiles").document().getId();
                newChild.setChildId(childId);
                Log.d(TAG, "STEP 3 - Generated new childId: " + childId);
            } else {
                Log.d(TAG, "STEP 3 - Using existing childId: " + childId);
            }

            // Test encryption
            Log.d(TAG, "STEP 4 - ENCRYPTION TEST:");
            String originalName = newChild.getName();
            String encryptedName = safeEncrypt(newChild.getEncryptedName(), originalName);
            String encryptedGender = safeEncrypt(newChild.getEncryptedGender(), newChild.getGender());
            String encryptedDisplayName = safeEncrypt(newChild.getEncryptedDisplayName(), newChild.getDisplayName());

            Log.d(TAG, "   - Original Name: " + originalName);
            Log.d(TAG, "   - Encrypted Name: " + encryptedName);
            Log.d(TAG, "   - Encrypted Gender: " + encryptedGender);
            Log.d(TAG, "   - Encrypted DisplayName: " + encryptedDisplayName);

            // Prepare Firestore document
            Log.d(TAG, "STEP 5 - FIRESTORE DOCUMENT DATA:");
            Map<String, Object> childData = new HashMap<>();
            childData.put("childId", childId);
            childData.put("parentId", parentId);
            childData.put("name", encryptedName);
            childData.put("gender", encryptedGender);
            childData.put("displayName", encryptedDisplayName);
            childData.put("age", newChild.getAge());
            childData.put("learningLevel", newChild.getLearningLevel());
            childData.put("active", true);
            childData.put("progress", 0);
            childData.put("stars", 0);
            childData.put("createdAt", FieldValue.serverTimestamp());

            // Log ALL document data
            for (Map.Entry<String, Object> entry : childData.entrySet()) {
                Log.d(TAG, "   - " + entry.getKey() + ": " + entry.getValue());
            }

            Log.d(TAG, "STEP 6 - ATTEMPTING FIRESTORE WRITE...");
            Log.d(TAG, "   Collection: child_profiles");
            Log.d(TAG, "   Document ID: " + childId);
            Log.d(TAG, "   Parent ID: " + parentId);

            final String finalChildId = childId;
            db.collection("child_profiles")
                    .document(finalChildId)
                    .set(childData)
                    .addOnSuccessListener(aVoid -> {
                        Log.i(TAG, "🎉 SUCCESS: Firestore document created: " + finalChildId);
                        Log.i(TAG, "✅ CHILD PROFILE SAVED SUCCESSFULLY!");
                        callback.onSuccess(finalChildId);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "FIRESTORE WRITE FAILED: " + e.getMessage());
                        Log.e(TAG, "Error details: ", e);
                        Log.e(TAG, "Check Firestore rules and internet connection");
                        callback.onFailure(e);
                    });

        } catch (Exception e) {
            Log.e(TAG, "UNEXPECTED EXCEPTION in saveChildProfile", e);
            callback.onFailure(e);
        }
    }

    /** Load all children for the current logged-in parent */
    public void getChildrenForCurrentParent(@NonNull DataCallbacks.ChildrenListCallback callback) {
        String parentId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (parentId == null) {
            Log.e(TAG, "❌ No user logged in - cannot fetch children");
            callback.onFailure(new Exception("Parent not logged in"));
            return;
        }

        Log.d(TAG, "🔍 Fetching child profiles for parentId=" + parentId);

        db.collection("child_profiles")
                .whereEqualTo("parentId", parentId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<ChildProfile> children = new ArrayList<>();
                    if (snapshot == null || snapshot.isEmpty()) {
                        Log.w(TAG, "📭 No child profiles found for parentId=" + parentId);
                        callback.onSuccess(children);
                        return;
                    }

                    Log.d(TAG, "📄 Found " + snapshot.size() + " child documents");

                    for (QueryDocumentSnapshot doc : snapshot) {
                        try {
                            ChildProfile child = new ChildProfile();
                            child.setChildId(doc.getString("childId"));
                            child.setParentId(doc.getString("parentId"));
                            child.setLearningLevel(doc.getString("learningLevel"));
                            child.setAge(doc.getLong("age") != null ? doc.getLong("age").intValue() : 0);
                            child.setActive(doc.getBoolean("active") != null ? doc.getBoolean("active") : true);
                            child.setProgress(doc.getLong("progress") != null ? doc.getLong("progress").intValue() : 0);
                            child.setStars(doc.getLong("stars") != null ? doc.getLong("stars").intValue() : 0);

                            // Decrypt safely (fallback to plain text)
                            child.setDecryptedName(doc.getString("name"));
                            child.setDecryptedGender(doc.getString("gender"));
                            child.setDecryptedDisplayName(doc.getString("displayName"));

                            children.add(child);
                            Log.d(TAG, "✅ Loaded child: " + child.getDisplayName() + " (ID: " + child.getChildId() + ")");
                        } catch (Exception e) {
                            Log.e(TAG, "❌ Failed to parse child document: " + doc.getId(), e);
                        }
                    }

                    Log.i(TAG, "✅ Successfully loaded " + children.size() + " children for parentId=" + parentId);
                    callback.onSuccess(children);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Firestore query failed for parentId=" + parentId, e);
                    callback.onFailure(e);
                });
    }

    /** Encryption fallback (avoids null crashes) */
    private String safeEncrypt(String encrypted, String fallback) {
        if (encrypted == null || encrypted.trim().isEmpty()) {
            Log.w(TAG, "⚠️ Encryption returned null/empty, falling back to plain text: " + fallback);
            return fallback != null ? fallback : "Unknown";
        }
        return encrypted;
    }
}