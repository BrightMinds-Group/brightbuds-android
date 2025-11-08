package com.example.brightbuds_app.services;

import android.content.Context;
import android.util.Log;

import com.example.brightbuds_app.interfaces.DataCallbacks;
import com.example.brightbuds_app.interfaces.ProgressListCallback;
import com.example.brightbuds_app.models.Progress;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProgressService {

    private static final String TAG = "ProgressService";
    private final FirebaseFirestore db;
    private final DatabaseHelper localDb;

    public ProgressService(Context context) {
        this.db = FirebaseFirestore.getInstance();
        this.localDb = new DatabaseHelper(context);
    }


    // PROGRESS RETRIEVAL
    public void getAllProgressForParentWithChildren(String parentId, List<String> childIds, ProgressListCallback callback) {
        if (childIds == null || childIds.isEmpty()) {
            Log.w(TAG, "⚠️ No child IDs provided for parent: " + parentId);
            callback.onSuccess(new ArrayList<>());
            return;
        }

        db.collection("child_progress")
                .whereEqualTo("parentId", parentId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Progress> result = new ArrayList<>();
                    Set<String> foundChildIds = new HashSet<>();

                    for (DocumentSnapshot doc : querySnapshot) {
                        Progress progress = doc.toObject(Progress.class);
                        if (progress != null) {
                            progress.setProgressId(doc.getId());
                            result.add(progress);
                            foundChildIds.add(progress.getChildId());
                            cacheProgressLocally(progress);
                        }
                    }
                    validateChildProgressConsistency(childIds, foundChildIds);
                    callback.onSuccess(result);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Firestore fetch failed (with children)", e);
                    callback.onFailure(e);
                });
    }


    // MODULE COMPLETION
    public void markModuleCompleted(String childId, String moduleId, int score, DataCallbacks.GenericCallback callback) {
        Log.d(TAG, "🎯 markModuleCompleted - Child: " + childId + ", Module: " + moduleId + ", Score: " + score);

        var user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.onFailure(new IllegalStateException("User not authenticated"));
            return;
        }

        final String parentId = user.getUid();
        Map<String, Object> progressData = createProgressData(parentId, childId, moduleId, score);

        db.collection("child_progress")
                .add(progressData)
                .addOnSuccessListener(docRef -> {
                    Log.i(TAG, "✅ Progress saved to Firestore (" + docRef.getId() + ")");
                    cacheProgressRecord(docRef.getId(), parentId, childId, moduleId, score, "completed");
                    callback.onSuccess("Progress saved!");
                })
                .addOnFailureListener(e -> handleProgressSaveFailure(e, parentId, childId, moduleId, score, callback));
    }


    // LOG VIDEO PLAY COUNTS
    public void logVideoPlay(String childId, String moduleId, DataCallbacks.GenericCallback callback) {
        Log.d(TAG, "🎥 logVideoPlay - Child: " + childId + ", Module: " + moduleId);

        var user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.onFailure(new IllegalStateException("User not authenticated"));
            return;
        }
        final String parentId = user.getUid();

        if (childId == null || moduleId == null) {
            callback.onFailure(new IllegalArgumentException("Missing childId or moduleId"));
            return;
        }

        String docId = childId + "_" + moduleId; // ensures consistent ID per module per child

        Map<String, Object> data = new HashMap<>();
        data.put("parentId", parentId);
        data.put("childId", childId);
        data.put("moduleId", moduleId);
        data.put("type", "video");
        data.put("status", "completed");
        data.put("completionStatus", true);
        data.put("lastUpdated", System.currentTimeMillis());
        data.put("timestamp", System.currentTimeMillis());
        data.put("score", 100);
        data.put("plays", FieldValue.increment(1)); // properly increments play count

        db.collection("child_progress")
                .document(docId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.i(TAG, "✅ Video play logged: " + docId);
                    cacheProgressRecord(docId, parentId, childId, moduleId, 100, "video_played");
                    callback.onSuccess("Video play recorded");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to log video play", e);
                    callback.onFailure(e);
                });
    }

    // COMPLETION PERCENTAGE UPDATE
    public void setCompletionPercentage(String parentId, String childId, String moduleId, int percentage, DataCallbacks.GenericCallback callback) {
        db.collection("child_progress")
                .whereEqualTo("parentId", parentId)
                .whereEqualTo("childId", childId)
                .whereEqualTo("moduleId", moduleId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty()) {
                        updateExistingProgress(snapshot, percentage, callback);
                    } else {
                        markModuleCompleted(childId, moduleId, percentage, callback);
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    // UTILITIES
    private Map<String, Object> createProgressData(String parentId, String childId, String moduleId, int score) {
        Map<String, Object> data = new HashMap<>();
        data.put("parentId", parentId);
        data.put("childId", childId);
        data.put("moduleId", moduleId);
        data.put("score", score);
        data.put("status", "completed");
        data.put("timestamp", System.currentTimeMillis());
        data.put("timeSpent", 0L);
        return data;
    }

    private void cacheProgressLocally(Progress progress) {
        localDb.insertOrUpdateProgress(
                progress.getProgressId(),
                progress.getParentId(),
                progress.getChildId(),
                progress.getModuleId(),
                (int) progress.getScore(),
                progress.getStatus(),
                System.currentTimeMillis(),
                progress.getTimeSpent()
        );
    }

    private void cacheProgressRecord(String docId, String parentId, String childId, String moduleId, int score, String status) {
        localDb.insertOrUpdateProgress(docId, parentId, childId, moduleId, score, status, System.currentTimeMillis(), 0L);
    }

    private void handleProgressSaveFailure(Exception e, String parentId, String childId, String moduleId, int score, DataCallbacks.GenericCallback callback) {
        Log.e(TAG, "❌ Firestore failed - saving locally", e);
        cacheProgressRecord(String.valueOf(System.currentTimeMillis()), parentId, childId, moduleId, score, "completed");
        callback.onSuccess("Saved locally (offline mode)");
    }

    private void validateChildProgressConsistency(List<String> expectedChildIds, Set<String> foundChildIds) {
        for (String childId : expectedChildIds) {
            if (!foundChildIds.contains(childId)) {
                Log.w(TAG, "⚠️ No progress found for child: " + childId);
            }
        }
    }

    private void updateExistingProgress(QuerySnapshot snapshot, int percentage, DataCallbacks.GenericCallback callback) {
        String docId = snapshot.getDocuments().get(0).getId();
        String status = percentage >= 100 ? "completed" : "in_progress";

        db.collection("child_progress").document(docId)
                .update("score", percentage, "status", status, "timestamp", System.currentTimeMillis())
                .addOnSuccessListener(aVoid -> {
                    Log.i(TAG, "✅ Updated progress record: " + docId);
                    callback.onSuccess("Progress updated!");
                })
                .addOnFailureListener(callback::onFailure);
    }

    public double calculateAverageScore(List<Progress> progressList) {
        if (progressList == null || progressList.isEmpty()) return 0;
        double total = 0;
        int count = 0;
        for (Progress p : progressList) {
            total += p.getScore();
            count++;
        }
        return count > 0 ? total / count : 0;
    }

    // Dummy method retained to avoid app crash from old references
    public void logVideoPlay() {
        Log.d(TAG, "⚠️ Deprecated logVideoPlay() called with no parameters.");
    }
}
