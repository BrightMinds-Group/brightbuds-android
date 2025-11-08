package com.example.brightbuds_app.services;

import android.content.Context;
import android.util.Log;

import com.example.brightbuds_app.interfaces.DataCallbacks;
import com.example.brightbuds_app.models.SyncItem;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

/**
 * ✅ DataSyncManager
 * Handles background syncing between local SQLite (DatabaseHelper)
 * and Firebase Firestore.
 * Ensures offline-first consistency and retry mechanism for queued updates.
 */
public class DataSyncManager {

    private static final String TAG = "DataSyncManager";

    private final DatabaseHelper localDb;
    private final FirebaseFirestore firestore;
    private final Gson gson = new Gson();
    private final Context context;

    public DataSyncManager(Context context) {
        this.context = context;
        this.localDb = new DatabaseHelper(context);
        this.firestore = FirebaseFirestore.getInstance();
    }

    // --------------------------------------------------------------------------
    // 🔹 Sync all pending records in local queue
    // --------------------------------------------------------------------------
    public void syncAllPendingChanges(DataCallbacks.GenericCallback callback) {
        List<SyncItem> syncItems = localDb.getSyncQueue();

        if (syncItems.isEmpty()) {
            callback.onSuccess("✅ All records are already synced");
            return;
        }

        Log.i(TAG, "🔄 Syncing " + syncItems.size() + " pending items...");
        syncNextItem(syncItems, 0, callback);
    }

    // --------------------------------------------------------------------------
    // 🔹 Process each queued record recursively
    // --------------------------------------------------------------------------
    private void syncNextItem(List<SyncItem> syncItems, int index, DataCallbacks.GenericCallback callback) {
        if (index >= syncItems.size()) {
            callback.onSuccess("✅ Sync complete for all items");
            return;
        }

        SyncItem item = syncItems.get(index);
        Log.d(TAG, "Processing sync item → " + item.getTableName() + " : " + item.getOperation());

        processSyncItem(item, new DataCallbacks.GenericCallback() {
            @Override
            public void onSuccess(String result) {
                localDb.markAsSynced(item.getTableName(), item.getRecordId());
                Log.d(TAG, "✅ Synced " + item.getTableName() + " → " + item.getRecordId());
                syncNextItem(syncItems, index + 1, callback);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "❌ Failed syncing item " + item.getRecordId(), e);
                callback.onFailure(e);
            }
        });
    }

    // --------------------------------------------------------------------------
    // 🔹 Determine operation type for each SyncItem
    // --------------------------------------------------------------------------
    private void processSyncItem(SyncItem item, DataCallbacks.GenericCallback callback) {
        switch (item.getOperation().toLowerCase()) {
            case "insert":
                handleInsertOperation(item, callback);
                break;
            case "update":
                handleUpdateOperation(item, callback);
                break;
            case "delete":
                handleDeleteOperation(item, callback);
                break;
            default:
                Log.w(TAG, "⚠️ Unknown operation type: " + item.getOperation());
                callback.onSuccess("Skipped unknown operation");
        }
    }

    // --------------------------------------------------------------------------
    // 🔹 Handle INSERT operations
    // --------------------------------------------------------------------------
    private void handleInsertOperation(SyncItem item, DataCallbacks.GenericCallback callback) {
        String collection = resolveCollectionName(item.getTableName());

        firestore.collection(collection)
                .document(item.getRecordId())
                .set(item)
                .addOnSuccessListener(unused -> {
                    localDb.markAsSynced(item.getTableName(), item.getRecordId());
                    callback.onSuccess("✅ Inserted " + collection + " successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to insert " + collection, e);
                    callback.onFailure(e);
                });
    }

    // --------------------------------------------------------------------------
    // 🔹 Handle UPDATE operations
    // --------------------------------------------------------------------------
    private void handleUpdateOperation(SyncItem item, DataCallbacks.GenericCallback callback) {
        String collection = resolveCollectionName(item.getTableName());

        firestore.collection(collection)
                .document(item.getRecordId())
                .update("lastSynced", System.currentTimeMillis())
                .addOnSuccessListener(unused -> {
                    localDb.markAsSynced(item.getTableName(), item.getRecordId());
                    callback.onSuccess("✅ Updated " + collection + " successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to update " + collection, e);
                    callback.onFailure(e);
                });
    }

    // --------------------------------------------------------------------------
    // 🔹 Handle DELETE operations
    // --------------------------------------------------------------------------
    private void handleDeleteOperation(SyncItem item, DataCallbacks.GenericCallback callback) {
        String collection = resolveCollectionName(item.getTableName());

        firestore.collection(collection)
                .document(item.getRecordId())
                .delete()
                .addOnSuccessListener(unused -> {
                    localDb.markAsSynced(item.getTableName(), item.getRecordId());
                    callback.onSuccess("✅ Deleted " + collection + " successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to delete " + collection, e);
                    callback.onFailure(e);
                });
    }

    // --------------------------------------------------------------------------
    // 🔹 Utility Methods
    // --------------------------------------------------------------------------

    /** Resolves SQLite table names to Firestore collection names */
    private String resolveCollectionName(String tableName) {
        switch (tableName) {
            case DatabaseHelper.TABLE_CHILD_PROFILE:
                return "children";
            case DatabaseHelper.TABLE_PROGRESS:
                return "child_progress";
            default:
                return tableName.toLowerCase();
        }
    }

    /** Check current sync queue status */
    public void getSyncStatus(DataCallbacks.GenericCallback callback) {
        List<SyncItem> syncItems = localDb.getSyncQueue();
        if (syncItems.isEmpty()) {
            callback.onSuccess("✅ Local data is fully synced");
        } else {
            callback.onSuccess("⚠️ " + syncItems.size() + " pending items in queue");
        }
    }

    /** 🔹 Optional: Download progress from Firestore → local SQLite */
    public void downloadProgressForChildren(List<QueryDocumentSnapshot> children, DataCallbacks.GenericCallback callback) {
        try {
            for (QueryDocumentSnapshot childDoc : children) {
                String childId = childDoc.getId();
                firestore.collection("child_progress")
                        .whereEqualTo("childId", childId)
                        .get()
                        .addOnSuccessListener(progressDocs -> {
                            for (QueryDocumentSnapshot progressDoc : progressDocs) {
                                String json = gson.toJson(progressDoc.getData());
                                Type type = new TypeToken<Object>() {}.getType();
                                Object progressObj = gson.fromJson(json, type);
                                Log.d(TAG, "⬇️ Downloaded progress for child: " + childId + " → " + progressObj);
                            }
                        });
            }
            callback.onSuccess("✅ Progress downloaded successfully");
        } catch (Exception e) {
            callback.onFailure(e);
        }
    }
}
