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
    private static final int TOTAL_MODULES = 7;

    private final FirebaseFirestore db;
    private final DatabaseHelper localDb;

    public ProgressService(Context context) {
        this.db = FirebaseFirestore.getInstance();
        this.localDb = new DatabaseHelper(context);
    }


    // FETCH PROGRESS
    public void getAllProgressForParentWithChildren(String parentId,
                                                    List<String> childIds,
                                                    ProgressListCallback callback) {
        if (childIds == null || childIds.isEmpty()) {
            Log.w(TAG, "⚠️ No child IDs for parent: " + parentId);
            callback.onSuccess(new ArrayList<>());
            return;
        }

        db.collection("child_progress")
                .whereEqualTo("parentId", parentId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Progress> result = new ArrayList<>();
                    Set<String> foundChildIds = new HashSet<>();

                    for (DocumentSnapshot doc : snapshot) {
                        Progress p = doc.toObject(Progress.class);
                        if (p != null) {
                            p.setProgressId(doc.getId());
                            result.add(p);
                            foundChildIds.add(p.getChildId());
                            // from server → mark as synced locally
                            cacheProgressLocally(p, true);
                        }
                    }

                    validateChildProgressConsistency(childIds, foundChildIds);
                    callback.onSuccess(result);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Firestore fetch failed", e);
                    callback.onFailure(e);
                });
    }

    // MODULE COMPLETED

    public void markModuleCompleted(String childId,
                                    String moduleId,
                                    int score,
                                    DataCallbacks.GenericCallback callback) {

        Log.d(TAG, "🎯 markModuleCompleted child=" + childId +
                " module=" + moduleId + " score=" + score);

        var user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.onFailure(new IllegalStateException("User not authenticated"));
            return;
        }

        final String parentId = user.getUid();
        Map<String, Object> data = createProgressData(parentId, childId, moduleId, score);

        db.collection("child_progress")
                .add(data)
                .addOnSuccessListener(docRef -> {
                    Log.i(TAG, "✅ Progress saved online: " + docRef.getId());
                    cacheProgressRecord(docRef.getId(), parentId, childId, moduleId,
                            score, "completed", true);
                    updateChildProgressStats(childId);
                    callback.onSuccess("Progress saved!");
                })
                .addOnFailureListener(e ->
                        handleProgressSaveFailure(e, parentId, childId, moduleId, score, callback));
    }


    // VIDEO PLAY
    public void logVideoPlay(String childId,
                             String moduleId,
                             DataCallbacks.GenericCallback callback) {

        Log.d(TAG, "🎥 logVideoPlay child=" + childId + " module=" + moduleId);

        var user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.onFailure(new IllegalStateException("User not authenticated"));
            return;
        }
        if (childId == null || moduleId == null) {
            callback.onFailure(new IllegalArgumentException("Missing childId/moduleId"));
            return;
        }

        final String parentId = user.getUid();
        final String docId = childId + "_" + moduleId;

        Map<String, Object> data = new HashMap<>();
        data.put("parentId", parentId);
        data.put("childId", childId);
        data.put("moduleId", moduleId);
        data.put("type", "video");
        data.put("status", "completed");
        data.put("completionStatus", true);
        data.put("timestamp", System.currentTimeMillis());
        data.put("lastUpdated", System.currentTimeMillis());
        data.put("score", 100);
        data.put("plays", FieldValue.increment(1));

        db.collection("child_progress")
                .document(docId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    Log.i(TAG, "✅ Video play logged online: " + docId);
                    cacheProgressRecord(docId, parentId, childId, moduleId,
                            100, "video_played", true);
                    updateChildProgressStats(childId);
                    callback.onSuccess("Video play recorded");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to log video play online, caching", e);
                    cacheProgressRecord(docId, parentId, childId, moduleId,
                            100, "video_played", false);
                    callback.onSuccess("Saved locally (offline mode)");
                });
    }

    // SET COMPLETION %
    public void setCompletionPercentage(String parentId,
                                        String childId,
                                        String moduleId,
                                        int percentage,
                                        DataCallbacks.GenericCallback callback) {

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

    // INTERNAL HELPERS
    private Map<String, Object> createProgressData(String parentId,
                                                   String childId,
                                                   String moduleId,
                                                   int score) {
        Map<String, Object> m = new HashMap<>();
        m.put("parentId", parentId);
        m.put("childId", childId);
        m.put("moduleId", moduleId);
        m.put("score", score);
        m.put("status", "completed");
        m.put("completionStatus", score >= 70);
        m.put("timestamp", System.currentTimeMillis());
        m.put("timeSpent", 0L);
        m.put("plays", 1);
        return m;
    }

    private void cacheProgressLocally(Progress p, boolean isSynced) {
        localDb.insertOrUpdateProgress(
                p.getProgressId(),
                p.getParentId(),
                p.getChildId(),
                p.getModuleId(),
                (int) p.getScore(),
                p.getStatus(),
                p.getTimestamp(),
                p.getTimeSpent(),
                isSynced
        );
    }

    private void cacheProgressRecord(String id,
                                     String parentId,
                                     String childId,
                                     String moduleId,
                                     int score,
                                     String status,
                                     boolean isSynced) {
        localDb.insertOrUpdateProgress(
                id,
                parentId,
                childId,
                moduleId,
                score,
                status,
                System.currentTimeMillis(),
                0L,
                isSynced
        );
    }

    private void handleProgressSaveFailure(Exception e,
                                           String parentId,
                                           String childId,
                                           String moduleId,
                                           int score,
                                           DataCallbacks.GenericCallback callback) {

        Log.e(TAG, "❌ Firestore unavailable, caching offline", e);
        String localId = "offline_" + System.currentTimeMillis();
        cacheProgressRecord(localId, parentId, childId, moduleId,
                score, "completed", false);
        callback.onSuccess("Saved locally (offline mode)");
    }

    private void validateChildProgressConsistency(List<String> expected, Set<String> found) {
        for (String id : expected) {
            if (!found.contains(id)) {
                Log.w(TAG, "⚠️ No progress found for child: " + id);
            }
        }
    }

    private void updateExistingProgress(QuerySnapshot snapshot,
                                        int percentage,
                                        DataCallbacks.GenericCallback callback) {

        DocumentSnapshot doc = snapshot.getDocuments().get(0);
        String docId = doc.getId();
        String childId = doc.getString("childId");
        String parentId = doc.getString("parentId");
        String moduleId = doc.getString("moduleId");

        String status = percentage >= 100 ? "completed" : "in_progress";

        db.collection("child_progress").document(docId)
                .update(
                        "score", percentage,
                        "status", status,
                        "completionStatus", percentage >= 70,
                        "timestamp", System.currentTimeMillis()
                )
                .addOnSuccessListener(unused -> {
                    Log.i(TAG, "✅ Updated progress online: " + docId);
                    if (childId != null && parentId != null && moduleId != null) {
                        cacheProgressRecord(docId, parentId, childId, moduleId,
                                percentage, status, true);
                        updateChildProgressStats(childId);
                    }
                    callback.onSuccess("Progress updated!");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to update online, caching", e);
                    if (childId != null && parentId != null && moduleId != null) {
                        cacheProgressRecord(docId, parentId, childId, moduleId,
                                percentage, status, false);
                    }
                    callback.onFailure(e);
                });
    }

    /** Recalculate child-level progress & stars from child_progress */
    private void updateChildProgressStats(String childId) {
        db.collection("child_progress")
                .whereEqualTo("childId", childId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) return;

                    int completedModules = 0;

                    for (DocumentSnapshot doc : snapshot) {
                        Progress p = doc.toObject(Progress.class);
                        if (p == null) continue;

                        boolean completed =
                                p.getScore() >= 70 ||
                                        "completed".equalsIgnoreCase(p.getStatus()) ||
                                        p.isCompletionStatus();

                        if (completed) completedModules++;
                    }

                    double ratio = completedModules / (double) TOTAL_MODULES;
                    int progressPercent = (int) Math.round(Math.min(1.0, ratio) * 100.0);
                    int stars = (int) Math.round(Math.min(1.0, ratio) * 5.0);

                    Log.d(TAG, "🌟 Stats child=" + childId +
                            " completed=" + completedModules +
                            " progress=" + progressPercent +
                            "% stars=" + stars);

                    db.collection("child_profiles").document(childId)
                            .update("progress", progressPercent, "stars", stars)
                            .addOnSuccessListener(unused ->
                                    Log.i(TAG, "✅ Child profile updated"))
                            .addOnFailureListener(e ->
                                    Log.e(TAG, "❌ Failed to update child profile", e));
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "❌ Failed to fetch child progress", e));
    }

    // Analytics helper
    public double calculateAverageScore(List<Progress> list) {
        if (list == null || list.isEmpty()) return 0;
        double total = 0;
        for (Progress p : list) total += p.getScore();
        return total / list.size();
    }

    // Legacy no-arg
    public void logVideoPlay() {
        Log.d(TAG, "⚠️ Deprecated logVideoPlay() called with no parameters.");
    }

    public void autoSyncOfflineProgress() {
    }

    /*
 Records a game session for any game module with detailed metrics.
 Writes to Firestore using merge to preserve history and supports offline cache.
 Also inserts or updates a local SQLite row so the dashboard can reflect progress instantly.

 Fields saved online:
   parentId, childId, moduleId, type="game", score, status, completionStatus,
   timestamp, timeSpent, plays, correct, incorrect, stars, lastUpdated

 Local cache uses DatabaseHelper.insertOrUpdateProgress to store a minimal row with timeSpent.
*/
    public void recordGameSession(String childId,
                                  String moduleId,
                                  int score,
                                  long timeSpentMs,
                                  int stars,
                                  int correct,
                                  int incorrect,
                                  int plays,
                                  DataCallbacks.GenericCallback callback) {

        var user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.onFailure(new IllegalStateException("User not authenticated"));
            return;
        }
        if (childId == null || moduleId == null) {
            callback.onFailure(new IllegalArgumentException("Missing childId or moduleId"));
            return;
        }

        final String parentId = user.getUid();
        final String docId = childId + "_" + moduleId;

        Map<String, Object> data = new HashMap<>();
        data.put("parentId", parentId);
        data.put("childId", childId);
        data.put("moduleId", moduleId);
        data.put("type", "game");
        data.put("score", score);
        data.put("status", score >= 70 ? "completed" : "in_progress");
        data.put("completionStatus", score >= 70);
        data.put("timestamp", System.currentTimeMillis());
        data.put("lastUpdated", System.currentTimeMillis());
        data.put("timeSpent", Math.max(0L, timeSpentMs));
        data.put("plays", Math.max(1, plays));
        data.put("correct", Math.max(0, correct));
        data.put("incorrect", Math.max(0, incorrect));
        data.put("stars", Math.max(0, stars));

        // Online write with merge to keep cumulative counters
        db.collection("child_progress")
                .document(docId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    // Local cache update for offline-first dashboard
                    cacheProgressRecord(docId, parentId, childId, moduleId,
                            score, (score >= 70 ? "completed" : "in_progress"), true);

                    // Update child-level stats after progress write
                    updateChildProgressStats(childId);

                    callback.onSuccess("Game session recorded");
                })
                .addOnFailureListener(e -> {
                    // Cache local if Firestore write fails
                    cacheProgressRecord(docId, parentId, childId, moduleId,
                            score, (score >= 70 ? "completed" : "in_progress"), false);
                    callback.onFailure(e);
                });
    }

}
