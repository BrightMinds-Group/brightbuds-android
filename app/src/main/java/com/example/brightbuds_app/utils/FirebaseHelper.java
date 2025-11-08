package com.example.brightbuds_app.utils;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseException;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

/**
 * FirebaseHelper — Singleton wrapper for managing all Firebase services in BrightBuds.
 * Centralized entry point for Auth, Firestore, Storage, Analytics, Crashlytics, and RemoteConfig.
 */
public class FirebaseHelper {

    private static final String TAG = "FirebaseHelper";
    private static FirebaseHelper instance;
    private final Context context;

    // Firebase services
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private FirebaseStorage storage;
    private FirebaseAnalytics analytics;
    private FirebaseCrashlytics crashlytics;
    private FirebaseRemoteConfig remoteConfig;

    // Flags
    private boolean isInitialized = false;
    private boolean useEmulator = false;
    private String emulatorHost = "10.0.2.2"; // Default Android emulator host

    private FirebaseHelper(Context context) {
        this.context = context.getApplicationContext();
        initializeFirebaseServices();
    }

    // =========================================================
    // 🔹 Singleton Access
    // =========================================================
    public static synchronized FirebaseHelper getInstance(Context context) {
        if (instance == null) instance = new FirebaseHelper(context);
        return instance;
    }

    public static synchronized FirebaseHelper getInstance() {
        if (instance == null)
            throw new IllegalStateException("FirebaseHelper must be initialized with context first");
        return instance;
    }

    // =========================================================
    // 🔹 Initialization
    // =========================================================
    private void initializeFirebaseServices() {
        try {
            FirebaseApp.initializeApp(context);
            initializeAuth();
            initializeFirestore();
            initializeStorage();
            initializeAnalytics();
            initializeCrashlytics();
            initializeRemoteConfig();
            isInitialized = true;
            Log.i(TAG, "✅ Firebase services initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to initialize Firebase services", e);
            isInitialized = false;
        }
    }

    private void initializeAuth() {
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseAuth.setLanguageCode("en");
        Log.d(TAG, "Firebase Auth initialized");
    }

    private void initializeFirestore() {
        firestore = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build();
        firestore.setFirestoreSettings(settings);
        Log.d(TAG, "Cloud Firestore initialized with offline persistence");
    }

    private void initializeStorage() {
        storage = FirebaseStorage.getInstance();
        storage.setMaxUploadRetryTimeMillis(Constants.STORAGE_MAX_RETRY_TIME);
        storage.setMaxOperationRetryTimeMillis(Constants.STORAGE_MAX_RETRY_TIME);
        Log.d(TAG, "Firebase Storage initialized");
    }

    private void initializeAnalytics() {
        analytics = FirebaseAnalytics.getInstance(context);
        if (isUserLoggedIn()) setAnalyticsUserProperties();
        Log.d(TAG, "Firebase Analytics initialized");
    }

    private void initializeCrashlytics() {
        crashlytics = FirebaseCrashlytics.getInstance();
        crashlytics.setCrashlyticsCollectionEnabled(!Constants.IS_DEBUG);
        if (isUserLoggedIn()) {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user != null) crashlytics.setUserId(user.getUid());
        }
        Log.d(TAG, "Firebase Crashlytics initialized");
    }

    private void initializeRemoteConfig() {
        remoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(Constants.IS_DEBUG ? 0 : 3600)
                .build();
        remoteConfig.setConfigSettingsAsync(configSettings);
        remoteConfig.setDefaultsAsync(Constants.getRemoteConfigDefaults());
        Log.d(TAG, "Firebase Remote Config initialized");
    }

    // =========================================================
    // 🔹 Emulator Setup
    // =========================================================
    public void setupEmulators() {
        if (!Constants.IS_DEBUG) {
            Log.w(TAG, "⚠️ Emulators should only be used in debug mode");
            return;
        }

        useEmulator = true;
        try {
            firestore.useEmulator(emulatorHost, 8080);
            firebaseAuth.useEmulator(emulatorHost, 9099);
            storage.useEmulator(emulatorHost, 9199);
            Log.i(TAG, "Firebase emulators configured for development");
        } catch (Exception e) {
            Log.e(TAG, "Failed to setup Firebase emulators", e);
        }
    }

    // =========================================================
    // 🔹 Public Accessors
    // =========================================================
    public FirebaseAuth getAuth() {
        requireInit();
        return firebaseAuth;
    }

    public FirebaseFirestore getFirestore() {
        requireInit();
        return firestore;
    }

    public FirebaseStorage getStorage() {
        requireInit();
        return storage;
    }

    public FirebaseAnalytics getAnalytics() {
        requireInit();
        return analytics;
    }

    public FirebaseCrashlytics getCrashlytics() {
        requireInit();
        return crashlytics;
    }

    public FirebaseRemoteConfig getRemoteConfig() {
        requireInit();
        return remoteConfig;
    }

    private void requireInit() {
        if (!isInitialized) throw new IllegalStateException("FirebaseHelper not initialized");
    }

    // =========================================================
    // 🔹 Utility Methods
    // =========================================================
    public boolean isInitialized() {
        return isInitialized;
    }

    public boolean isUserLoggedIn() {
        return firebaseAuth != null && firebaseAuth.getCurrentUser() != null;
    }

    public String getCurrentUserId() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        return (user != null) ? user.getUid() : null;
    }

    public String getCurrentUserEmail() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        return (user != null) ? user.getEmail() : null;
    }

    public String getCurrentUserName() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        return (user != null && user.getDisplayName() != null)
                ? user.getDisplayName() : "User";
    }

    // =========================================================
    // 🔹 Analytics Utilities
    // =========================================================
    public void setAnalyticsUserProperties() {
        if (analytics == null || !isUserLoggedIn()) return;
        FirebaseUser user = firebaseAuth.getCurrentUser();
        analytics.setUserProperty("user_type", Constants.ROLE_PARENT);
        if (user != null && user.getMetadata() != null)
            analytics.setUserProperty("account_created",
                    String.valueOf(user.getMetadata().getCreationTimestamp()));
        Log.d(TAG, "Analytics user properties set");
    }

    public void logEvent(String eventName, String paramName, String paramValue) {
        if (analytics == null) return;
        Bundle bundle = new Bundle();
        bundle.putString(paramName, paramValue);
        analytics.logEvent(eventName, bundle);
        Log.d(TAG, "Logged event: " + eventName);
    }

    public void logEvent(String eventName, Bundle parameters) {
        if (analytics == null) return;
        analytics.logEvent(eventName, parameters);
        Log.d(TAG, "Logged event: " + eventName + " with " + parameters.size() + " parameters");
    }

    public void logScreenView(String screenName, String screenClass) {
        if (analytics == null) return;
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName);
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass);
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
        Log.d(TAG, "Screen view logged: " + screenName);
    }

    // =========================================================
    // 🔹 Crashlytics Utilities
    // =========================================================
    public void recordError(Exception e, String message) {
        if (crashlytics == null) return;
        crashlytics.log(message);
        crashlytics.recordException(e);
        Log.e(TAG, "Recorded error: " + message, e);
    }

    public void setCrashlyticsKey(String key, String value) {
        if (crashlytics != null) crashlytics.setCustomKey(key, value);
    }

    // =========================================================
    // 🔹 Remote Config Utilities
    // =========================================================
    public void fetchRemoteConfig(ConfigFetchCallback callback) {
        if (remoteConfig == null) {
            callback.onFailure(new FirebaseException("Remote Config not initialized"));
            return;
        }

        remoteConfig.fetchAndActivate().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                boolean updated = task.getResult();
                Log.d(TAG, "Remote Config fetched successfully. Updated: " + updated);
                callback.onSuccess(updated);
            } else {
                Log.e(TAG, "Remote Config fetch failed", task.getException());
                callback.onFailure(task.getException());
            }
        });
    }

    public String getRemoteConfigString(String key) {
        return remoteConfig != null ? remoteConfig.getString(key) : "";
    }

    public boolean getRemoteConfigBoolean(String key) {
        return remoteConfig != null && remoteConfig.getBoolean(key);
    }

    public long getRemoteConfigLong(String key) {
        return remoteConfig != null ? remoteConfig.getLong(key) : 0L;
    }

    public double getRemoteConfigDouble(String key) {
        return remoteConfig != null ? remoteConfig.getDouble(key) : 0.0;
    }

    // =========================================================
    // 🔹 Storage References
    // =========================================================
    public StorageReference getChildImagesReference() {
        return storage.getReference().child("child_images");
    }

    public StorageReference getFamilyPhotosReference() {
        return storage.getReference().child("family_photos");
    }

    public StorageReference getReportsReference() {
        return storage.getReference().child("reports");
    }

    // =========================================================
    // 🔹 Cache and Cleanup
    // =========================================================
    public void clearCaches() {
        try {
            if (firestore != null) {
                firestore.terminate();
                firestore.clearPersistence();
                initializeFirestore();
            }
            Log.d(TAG, "Firebase caches cleared");
        } catch (Exception e) {
            Log.e(TAG, "Error clearing Firestore caches", e);
        }
    }

    public void cleanup() {
        instance = null;
        Log.d(TAG, "FirebaseHelper cleaned up");
    }

    // =========================================================
    // 🔹 Accessor Flags
    // =========================================================
    public boolean isUsingEmulator() {
        return useEmulator;
    }

    public String getEmulatorHost() {
        return emulatorHost;
    }

    public void setEmulatorHost(String host) {
        this.emulatorHost = host;
    }

    // =========================================================
    // 🔹 Callback Interfaces
    // =========================================================
    public interface ConfigFetchCallback {
        void onSuccess(boolean updated);
        void onFailure(Exception e);
    }

    public interface FirebaseOperationCallback {
        void onSuccess();
        void onFailure(Exception e);
    }
}
