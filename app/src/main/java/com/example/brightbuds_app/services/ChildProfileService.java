package com.example.brightbuds_app.services;

import android.util.Log;
import androidx.annotation.NonNull;

import com.example.brightbuds_app.interfaces.DataCallbacks;
import com.example.brightbuds_app.models.ChildProfile;
import com.example.brightbuds_app.models.Progress;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles CRUD operations for Child Profiles (Firestore + Encryption fallback)
 * and automatically calculates progress & stars based on completed modules.
 */
public class ChildProfileService {

    private static final String TAG = "ChildProfileService";
    private static final int TOTAL_MODULES = 7;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    /** Save child profile securely */
    public void saveChildProfile(@NonNull ChildProfile newChild, @NonNull DataCallbacks.GenericCallback callback) {
        Log.d(TAG, "🎯 Saving child profile for parentId=" + newChild.getParentId());

        try {
            String parentId = newChild.getParentId();
            if (parentId == null || parentId.isEmpty()) {
                callback.onFailure(new IllegalStateException("ParentId missing"));
                return;
            }

            String childId = newChild.getChildId();
            if (childId == null || childId.isEmpty()) {
                childId = db.collection("child_profiles").document().getId();
                newChild.setChildId(childId);
            }

            // Encrypt sensitive fields
            String encryptedName = safeEncrypt(newChild.getEncryptedName(), newChild.getName());
            String encryptedGender = safeEncrypt(newChild.getEncryptedGender(), newChild.getGender());
            String encryptedDisplayName = safeEncrypt(newChild.getEncryptedDisplayName(), newChild.getDisplayName());

            // Firestore data map
            Map<String, Object> childData = new HashMap<>();
            childData.put("childId", childId);
            childData.put("parentId", parentId);
            childData.put("name", encryptedName);
            childData.put("gender", encryptedGender);
            childData.put("displayName", encryptedDisplayName);
            childData.put("age", newChild.getAge());
            childData.put("learningLevel", newChild.getLearningLevel());
            childData.put("active", true);
            childData.put("completedModules", 0);
            childData.put("progress", 0);
            childData.put("stars", 0);
            childData.put("createdAt", FieldValue.serverTimestamp());

            final String finalChildId = childId;
            db.collection("child_profiles")
                    .document(finalChildId)
                    .set(childData)
                    .addOnSuccessListener(aVoid -> {
                        Log.i(TAG, "✅ Child profile created successfully (" + finalChildId + ")");
                        callback.onSuccess(finalChildId);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "❌ Failed to save child profile: " + e.getMessage());
                        callback.onFailure(e);
                    });


        } catch (Exception e) {
            Log.e(TAG, "❌ Unexpected exception saving child profile", e);
            callback.onFailure(e);
        }
    }

    /** Fetch all children and compute progress from child_progress */
    public void getChildrenForCurrentParent(@NonNull DataCallbacks.ChildrenListCallback callback) {
        String parentId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (parentId == null) {
            callback.onFailure(new Exception("Parent not logged in"));
            return;
        }

        Log.d(TAG, "🔍 Fetching children for parentId=" + parentId);

        db.collection("child_profiles")
                .whereEqualTo("parentId", parentId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<ChildProfile> children = new ArrayList<>();
                    if (snapshot == null || snapshot.isEmpty()) {
                        callback.onSuccess(children);
                        return;
                    }

                    for (QueryDocumentSnapshot doc : snapshot) {
                        try {
                            ChildProfile child = new ChildProfile();
                            child.setChildId(doc.getString("childId"));
                            child.setParentId(doc.getString("parentId"));
                            child.setLearningLevel(doc.getString("learningLevel"));
                            child.setAge(doc.getLong("age") != null ? doc.getLong("age").intValue() : 0);
                            child.setActive(doc.getBoolean("active") != null ? doc.getBoolean("active") : true);
                            child.setDecryptedName(doc.getString("name"));
                            child.setDecryptedGender(doc.getString("gender"));
                            child.setDecryptedDisplayName(doc.getString("displayName"));

                            children.add(child);
                        } catch (Exception e) {
                            Log.e(TAG, "❌ Error parsing child document: " + doc.getId(), e);
                        }
                    }

                    if (children.isEmpty()) {
                        callback.onSuccess(children);
                    } else {
                        Log.d(TAG, "📊 Loaded " + children.size() + " children; computing module progress...");
                        computeProgressForChildren(children, callback);
                    }

                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Firestore query failed for parentId=" + parentId, e);
                    callback.onFailure(e);
                });
    }

    /** Compute progress from child_progress collection */
    private void computeProgressForChildren(List<ChildProfile> children, DataCallbacks.ChildrenListCallback callback) {
        db.collection("child_progress")
                .get()
                .addOnSuccessListener(progressSnap -> {
                    for (ChildProfile child : children) {
                        int completed = 0;

                        for (var doc : progressSnap.getDocuments()) {
                            Progress progress = doc.toObject(Progress.class);
                            if (progress != null && progress.getChildId() != null &&
                                    progress.getChildId().equals(child.getChildId()) &&
                                    progress.isModuleCompleted()) {
                                completed++;
                            }
                        }

                        // Calculate % and stars from completed modules (max = 7)
                        child.setCompletedModules(completed);
                        Log.d(TAG, "✅ " + child.getDisplayName() + " completed " +
                                completed + "/" + TOTAL_MODULES + " modules → " +
                                child.getProgress() + "% | " + child.getStars() + " stars");

                        // Optionally sync updated progress to Firestore
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("completedModules", completed);
                        updates.put("progress", child.getProgress());
                        updates.put("stars", child.getStars());
                        db.collection("child_profiles").document(child.getChildId()).update(updates);
                    }

                    callback.onSuccess(children);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to fetch progress data", e);
                    callback.onFailure(e);
                });
    }

    /** Safe encryption fallback */
    private String safeEncrypt(String encrypted, String fallback) {
        if (encrypted == null || encrypted.trim().isEmpty()) {
            Log.w(TAG, "⚠️ Encryption returned null/empty, falling back to plain text: " + fallback);
            return fallback != null ? fallback : "Unknown";
        }
        return encrypted;
    }
}
