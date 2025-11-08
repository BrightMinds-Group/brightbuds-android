package com.example.brightbuds_app.services;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageException;

import java.io.File;

/*
 Handles Firebase Storage downloads and local caching.
 */
public class StorageService {

    private static final String TAG = "StorageService";
    private static StorageService instance;
    private final FirebaseStorage storage;

    private StorageService() {
        storage = FirebaseStorage.getInstance();
    }

    public static synchronized StorageService getInstance() {
        if (instance == null) instance = new StorageService();
        return instance;
    }

    public interface OnSuccessCallback {
        void onSuccess(Uri uri);
    }

    public interface OnFailureCallback {
        void onFailure(Exception e);
    }

    /*
     Downloads file from Firebase Storage if not cached locally.
     Works for both Glide/ExoPlayer and cached access.

     @param path Firebase Storage path (e.g., "avatars/{uid}/photo.jpg")
     */
    public void getOrDownloadFile(Context context, String path,
                                  OnSuccessCallback onSuccess,
                                  OnFailureCallback onFailure) {
        try {
            // Clean path for cache file name
            String safeName = path.replaceAll("[^a-zA-Z0-9._-]", "_");
            File localFile = new File(context.getCacheDir(), safeName);

            if (localFile.exists() && localFile.length() > 0) {
                Log.d(TAG, "Cache hit: " + safeName);
                onSuccess.onSuccess(Uri.fromFile(localFile));
                return;
            }

            Log.d(TAG, "Downloading from Firebase: " + path);
            StorageReference ref = storage.getReference().child(path);

            ref.getFile(localFile)
                    .addOnSuccessListener(task -> {
                        Log.d(TAG, "File downloaded: " + localFile.getAbsolutePath());
                        onSuccess.onSuccess(Uri.fromFile(localFile));
                    })
                    .addOnFailureListener(e -> {
                        // Clean up partial downloads
                        if (localFile.exists()) localFile.delete();

                        // Fallback: try HTTPS URL (e.g., for Glide)
                        if (e instanceof StorageException &&
                                ((StorageException) e).getErrorCode() == StorageException.ERROR_OBJECT_NOT_FOUND) {
                            Log.w(TAG, "File not found in Firebase Storage: " + path);
                        } else {
                            Log.e(TAG, "Download failed: " + e.getMessage());
                        }

                        ref.getDownloadUrl()
                                .addOnSuccessListener(onSuccess::onSuccess)
                                .addOnFailureListener(err -> {
                                    Log.e(TAG, "Fallback getDownloadUrl() failed: " + err.getMessage());
                                    onFailure.onFailure(err);
                                });
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error in getOrDownloadFile: " + e.getMessage());
            onFailure.onFailure(e);
        }
    }
}
