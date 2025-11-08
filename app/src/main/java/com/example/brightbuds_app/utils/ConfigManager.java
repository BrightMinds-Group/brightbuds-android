package com.example.brightbuds_app.utils;

import android.content.Context;
import android.util.Log;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.util.Map;

/**
 * ConfigManager
 * Handles all configuration logic by combining:
 *  🔹 Firebase Remote Config (cloud-managed dynamic settings)
 *  🔹 SecurePreferences (encrypted local storage & offline fallback)
 *
 * Ensures BrightBuds always has valid configuration data,
 * even when offline or Firebase fetch fails.
 */
public class ConfigManager {

    private static final String TAG = "ConfigManager";
    private static ConfigManager instance;

    private final Context context;
    private final SecurePreferences securePrefs;
    private final FirebaseRemoteConfig remoteConfig;

    private boolean isInitialized = false;

    private ConfigManager(Context context) {
        this.context = context.getApplicationContext();
        this.securePrefs = new SecurePreferences(this.context);
        this.remoteConfig = FirebaseRemoteConfig.getInstance();
        initializeRemoteConfig();
    }

    /**
     * Singleton instance
     */
    public static synchronized ConfigManager getInstance(Context context) {
        if (instance == null) instance = new ConfigManager(context);
        return instance;
    }

    // =========================================================
    // 🔹 Initialization
    // =========================================================
    private void initializeRemoteConfig() {
        try {
            FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                    .setMinimumFetchIntervalInSeconds(Constants.IS_DEBUG ? 0 : 3600)
                    .build();

            remoteConfig.setConfigSettingsAsync(configSettings);
            remoteConfig.setDefaultsAsync(Constants.getRemoteConfigDefaults());

            isInitialized = true;
            Log.i(TAG, "Firebase Remote Config initialized");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Remote Config", e);
            isInitialized = false;
        }
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    // =========================================================
    // 🔹 Fetch & Sync Methods
    // =========================================================
    /**
     * Fetch the latest Remote Config values from Firebase
     */
    public void fetchAndActivate(FetchCallback callback) {
        if (!isInitialized) {
            callback.onFailure(new IllegalStateException("ConfigManager not initialized"));
            return;
        }

        remoteConfig.fetchAndActivate().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.i(TAG, "✅ Remote Config fetched and activated");
                persistRemoteConfigValues(); // Save to local encrypted storage
                callback.onSuccess(true);
            } else {
                Log.w(TAG, "⚠️ Remote Config fetch failed — using local cache");
                callback.onFailure(task.getException());
            }
        });
    }

    /**
     * Persist all current Remote Config values to SecurePreferences
     */
    private void persistRemoteConfigValues() {
        try {
            Map<String, Object> allKeys = Constants.getRemoteConfigDefaults();

            for (String key : allKeys.keySet()) {
                Object value = remoteConfig.getValue(key).getClass();
                if (value instanceof Boolean)
                    securePrefs.putBoolean(key, (Boolean) value);
                else if (value instanceof Number)
                    securePrefs.putLong(key, ((Number) value).longValue());
                else
                    securePrefs.putString(key, value.toString());
            }

            Log.d(TAG, "Remote Config values persisted locally");
        } catch (Exception e) {
            Log.e(TAG, "Error persisting Remote Config values", e);
        }
    }

    // =========================================================
    // 🔹 Getters with Cloud + Local Fallback
    // =========================================================
    public boolean getBoolean(String key, boolean defaultValue) {
        try {
            return remoteConfig.getBoolean(key);
        } catch (Exception e) {
            return securePrefs.getBoolean(key, defaultValue);
        }
    }

    public String getString(String key, String defaultValue) {
        try {
            return remoteConfig.getString(key);
        } catch (Exception e) {
            return securePrefs.getString(key, defaultValue);
        }
    }

    public long getLong(String key, long defaultValue) {
        try {
            return remoteConfig.getLong(key);
        } catch (Exception e) {
            return securePrefs.getLong(key, defaultValue);
        }
    }

    public double getDouble(String key, double defaultValue) {
        try {
            return remoteConfig.getDouble(key);
        } catch (Exception e) {
            String localVal = securePrefs.getString(key, String.valueOf(defaultValue));
            try {
                return Double.parseDouble(localVal);
            } catch (NumberFormatException ex) {
                return defaultValue;
            }
        }
    }

    // =========================================================
    // 🔹 Common Config Accessors (app-level)
    // =========================================================
    public boolean isAutoReportEnabled() {
        return getBoolean(Constants.REMOTE_AUTO_REPORT_ENABLED, Constants.AUTO_GENERATE_REPORTS);
    }

    public int getDailySessionLimit() {
        return (int) getLong(Constants.REMOTE_DAILY_SESSION_LIMIT, Constants.DAILY_SESSION_LIMIT);
    }

    public int getWeeklyReportDay() {
        return (int) getLong(Constants.REMOTE_WEEKLY_REPORT_DAY, 0); // 0 = Sunday
    }

    public int getSessionTimeLimit() {
        return (int) getLong(Constants.REMOTE_SESSION_TIME_LIMIT, Constants.MAX_SESSION_TIME_MINUTES);
    }

    public boolean isFamilyModuleEnabled() {
        return getBoolean(Constants.REMOTE_FEATURE_FLAG_FAMILY_MODULE, true);
    }

    public boolean isAdvancedAnalyticsEnabled() {
        return getBoolean(Constants.REMOTE_FEATURE_FLAG_ADVANCED_ANALYTICS, false);
    }

    // =========================================================
    // 🔹 Local Preference Overrides
    // =========================================================
    public void setAutoSyncEnabled(boolean enabled) {
        securePrefs.putBoolean(Constants.PREF_AUTO_SYNC_ENABLED, enabled);
    }

    public boolean isAutoSyncEnabled() {
        return securePrefs.getBoolean(Constants.PREF_AUTO_SYNC_ENABLED, true);
    }

    public void setNotificationsEnabled(boolean enabled) {
        securePrefs.putBoolean(Constants.PREF_NOTIFICATIONS_ENABLED, enabled);
    }

    public boolean isNotificationsEnabled() {
        return securePrefs.getBoolean(Constants.PREF_NOTIFICATIONS_ENABLED, true);
    }

    public void setLastSyncTime(long timestamp) {
        securePrefs.putLong(Constants.PREF_LAST_SYNC_TIME, timestamp);
    }

    public long getLastSyncTime() {
        return securePrefs.getLong(Constants.PREF_LAST_SYNC_TIME, 0L);
    }

    // =========================================================
    // 🔹 Maintenance
    // =========================================================
    public void clearLocalCache() {
        securePrefs.clearAll();
        Log.i(TAG, "Local Config cache cleared");
    }

    // =========================================================
    // 🔹 Callback Interface
    // =========================================================
    public interface FetchCallback {
        void onSuccess(boolean updated);
        void onFailure(Exception e);
    }
}
