package com.example.brightbuds_app.services;

import android.util.Log;

import com.example.brightbuds_app.models.Module;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class ModuleService {

    public interface ModulesCallback {
        void onSuccess(List<Module> modules);
        void onError(Exception e);
    }

    private static final String TAG = "ModuleService";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /*
     Fetch all active modules ordered by "order" if available
     Automatically falls back to ordering by "title" if no index exists.
     Ensures null or missing 'isActive' modules are still shown.
     */
    public void getAllModules(ModulesCallback callback) {
        Log.d(TAG, "🔍 Fetching all active modules...");

        // Use safe query: avoids index errors if "order" field is missing
        Query query = db.collection("modules")
                .orderBy("title", Query.Direction.ASCENDING);

        query.get()
                .addOnSuccessListener(snapshot -> {
                    List<Module> modules = new ArrayList<>();
                    Log.d(TAG, "📦 Firestore returned " + snapshot.size() + " documents");

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        try {
                            Module module = doc.toObject(Module.class);
                            if (module != null) {

                                // Always ensure module ID is set
                                if (module.getId() == null || module.getId().isEmpty()) {
                                    module.setId(doc.getId());
                                }

                                // Ensure null isActive defaults to true
                                if (module.getIsActive() == null) {
                                    module.setIsActive(true);
                                }

                                if (!module.isActive()) {
                                    Log.d(TAG, "⏸️ Skipping inactive module: " + module.getTitle());
                                    continue;
                                }

                                modules.add(module);
                                Log.d(TAG, String.format(
                                        "✅ Loaded module: %s | Active: %s | ID: %s",
                                        module.getTitle(), module.isActive(), module.getId()
                                ));
                            } else {
                                Log.w(TAG, "⚠️ Null module object for document: " + doc.getId());
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "❌ Error converting document " + doc.getId(), e);
                        }
                    }

                    Log.i(TAG, "🎯 Successfully loaded " + modules.size() + " active modules");
                    callback.onSuccess(modules);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to load modules, falling back to unfiltered list", e);

                    // If index issue occurs (e.g., FAILED_PRECONDITION), retry without filters
                    db.collection("modules")
                            .get()
                            .addOnSuccessListener(snapshot -> {
                                List<Module> modules = new ArrayList<>();
                                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                                    Module module = doc.toObject(Module.class);
                                    if (module != null) {
                                        if (module.getId() == null || module.getId().isEmpty())
                                            module.setId(doc.getId());
                                        if (module.getIsActive() == null || module.getIsActive())
                                            modules.add(module);
                                    }
                                }
                                Log.w(TAG, "⚠️ Fallback loaded " + modules.size() + " modules (no order applied)");
                                callback.onSuccess(modules);
                            })
                            .addOnFailureListener(inner -> {
                                Log.e(TAG, "❌ Even fallback failed", inner);
                                callback.onError(inner);
                            });
                });
    }

    /*
     Debug fetch: get ALL modules (active + inactive)
     Used for testing or admin preview.
     */
    public void getAllModulesDebug(ModulesCallback callback) {
        Log.d(TAG, "🔍 [DEBUG] Fetching ALL modules (including inactive)...");

        db.collection("modules")
                .orderBy("title", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Module> modules = new ArrayList<>();
                    Log.d(TAG, "📦 [DEBUG] Firestore returned " + snapshot.size() + " total documents");

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        try {
                            Module module = doc.toObject(Module.class);
                            if (module != null) {
                                if (module.getId() == null || module.getId().isEmpty()) {
                                    module.setId(doc.getId());
                                }
                                modules.add(module);
                                Log.d(TAG, String.format("📋 [DEBUG] %s | Active: %s | ID: %s",
                                        module.getTitle(), module.isActive(), doc.getId()));
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "⚠️ [DEBUG] Conversion failed for " + doc.getId(), e);
                        }
                    }

                    Log.i(TAG, "🎯 [DEBUG] Loaded " + modules.size() + " total modules");
                    callback.onSuccess(modules);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ [DEBUG] Failed to fetch all modules", e);
                    callback.onError(e);
                });
    }
}
