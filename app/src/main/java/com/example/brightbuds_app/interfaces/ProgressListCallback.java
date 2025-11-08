package com.example.brightbuds_app.interfaces;

import com.example.brightbuds_app.models.Progress;
import java.util.List;

/**
 * Callback interface for returning progress data asynchronously
 * from Firestore queries.
 */
public interface ProgressListCallback {
    /**
     * Called when progress data is successfully fetched.
     * @param progressList List of Progress objects from Firestore
     */
    void onSuccess(List<Progress> progressList);

    /**
     * Called when fetching progress data fails.
     * @param e Exception thrown during the operation
     */
    void onFailure(Exception e);
}
