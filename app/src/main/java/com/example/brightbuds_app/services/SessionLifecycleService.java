package com.example.brightbuds_app.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

// Tracks app session duration for analytics
public class SessionLifecycleService extends Service {

    private static final String TAG = "SessionLifecycleService";
    private long sessionStartTime;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Session service created");
        sessionStartTime = System.currentTimeMillis();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Session service started");

        // You can add session tracking logic here

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        long sessionDuration = System.currentTimeMillis() - sessionStartTime;
        Log.d(TAG, "Session ended. Duration: " + sessionDuration + "ms");

        // Here you can save session data to Firebase or local database
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // Optional: Method to save session analytics
    private void saveSessionAnalytics(long duration) {
        // Implement your analytics saving logic here
        // This could save to Firestore, Analytics, or local database
        Log.d(TAG, "Saving session analytics: " + duration + "ms");
    }
}