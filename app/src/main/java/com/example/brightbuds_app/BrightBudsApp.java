package com.example.brightbuds_app;

import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

/**
 * BrightBudsApp
 * Initializes Firebase and handles global app configuration.
 * This ensures Firebase is always ready before any Activity runs.
 */
public class BrightBudsApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            // Initialize Firebase (safe even if it's already initialized)
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this);
            }
            Log.i("BrightBudsApp", "✅ Firebase initialized successfully");
        } catch (Exception e) {
            Log.e("BrightBudsApp", "❌ Firebase initialization failed", e);
        }

        // Optional: You can add crash logging, analytics, or performance monitoring here.
    }
}
