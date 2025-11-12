package com.example.brightbuds_app.utils;

import android.content.Context;
import android.util.Log;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.util.Map;

/**
 * ConfigManager
 * Handles configuration by combining Firebase Remote Config and SecurePreferences.
 * Provides cloud values with local encrypted fallback.
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

    public static synchronized ConfigManager getInstance(Context context) {
        if (instance == null) instance = new ConfigManager(context);
        return instance;
    }

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

    public void fetchAndActivate(FetchCallback callback) {
        if (!isInitialized) {
            callback.onFailure(new IllegalStateException("ConfigManager not initialized"));
            return;
        }

        remoteConfig.fetchAndActivate().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.i(TAG, "Remote Config fetched and activated");
                persistRemoteConfigValues();
                callback.onSuccess(true);
            } else {
                Log.w(TAG, "Remote Config fetch failed. Using local cache");
                callback.onFailure(task.getException());
            }
        });
    }

    // Correct persistence using types from defaults map
    private void persistRemoteConfigValues() {
        try {
            Map<String, Object> defaults = Constants.getRemoteConfigDefaults();
            for (String key : defaults.keySet()) {
                Object def = defaults.get(key);
                if (def instanceof Boolean) {
                    securePrefs.putBoolean(key, remoteConfig.getBoolean(key));
                } else if (def instanceof Number) {
                    long val = (long) remoteConfig.getDouble(key);
                    securePrefs.putLong(key, val);
                } else {
                    securePrefs.putString(key, remoteConfig.getString(key));
                }
            }
            Log.d(TAG, "Remote Config values persisted locally");
        } catch (Exception e) {
            Log.e(TAG, "Error persisting Remote Config values", e);
        }
    }

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

    public boolean isAutoReportEnabled() {
        return getBoolean(Constants.REMOTE_AUTO_REPORT_ENABLED, Constants.AUTO_GENERATE_REPORTS);
    }

    public int getDailySessionLimit() {
        return (int) getLong(Constants.REMOTE_DAILY_SESSION_LIMIT, Constants.DAILY_SESSION_LIMIT);
    }

    public int getWeeklyReportDay() {
        return (int) getLong(Constants.REMOTE_WEEKLY_REPORT_DAY, 0);
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

    public void clearLocalCache() {
        securePrefs.clearAll();
        Log.i(TAG, "Local Config cache cleared");
    }

    public interface FetchCallback {
        void onSuccess(boolean updated);
        void onFailure(Exception e);
    }
}
