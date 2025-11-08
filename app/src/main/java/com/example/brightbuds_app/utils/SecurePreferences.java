package com.example.brightbuds_app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/*
 SecurePreferences - A wrapper around SharedPreferences that encrypts/decrypts data using EncryptionUtil before saving or retrieving.

 Used for securely storing sensitive user or app settings such as:
 - Login state
 - Last sync timestamp
 - Report preferences
 - Notification and sound settings
 */
public class SecurePreferences {

    private static final String TAG = "SecurePreferences";
    private static final String PREF_NAME = "brightbuds_secure_prefs";

    private final SharedPreferences sharedPreferences;

    public SecurePreferences(Context context) {
        this.sharedPreferences = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // Generic Save Methods
    public void putString(String key, String value) {
        try {
            String encryptedValue = EncryptionUtil.encrypt(value);
            sharedPreferences.edit().putString(key, encryptedValue).apply();
        } catch (Exception e) {
            Log.e(TAG, "Failed to save encrypted string for key: " + key, e);
        }
    }

    public void putBoolean(String key, boolean value) {
        putString(key, String.valueOf(value));
    }

    public void putInt(String key, int value) {
        putString(key, String.valueOf(value));
    }

    public void putLong(String key, long value) {
        putString(key, String.valueOf(value));
    }

    // Generic Get Methods
    public String getString(String key, String defaultValue) {
        try {
            String encryptedValue = sharedPreferences.getString(key, null);
            if (encryptedValue == null) return defaultValue;

            String decryptedValue = EncryptionUtil.decrypt(encryptedValue);
            return decryptedValue != null ? decryptedValue : defaultValue;
        } catch (Exception e) {
            Log.e(TAG, "Failed to decrypt string for key: " + key, e);
            return defaultValue;
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String value = getString(key, String.valueOf(defaultValue));
        return Boolean.parseBoolean(value);
    }

    public long getLong(String key, long defaultValue) {
        String value = getString(key, String.valueOf(defaultValue));
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    // Utility Methods
    public boolean contains(String key) {
        return sharedPreferences.contains(key);
    }

    public void remove(String key) {
        sharedPreferences.edit().remove(key).apply();
        Log.d(TAG, "Removed key: " + key);
    }

    public void clearAll() {
        sharedPreferences.edit().clear().apply();
        Log.i(TAG, "All secure preferences cleared");
    }

    /*
     For debugging: shows decrypted values of all keys
     Use only in development
     */
    public void logAll() {
        for (String key : sharedPreferences.getAll().keySet()) {
            String decrypted = getString(key, "N/A");
            Log.d(TAG, "Key: " + key + " | Value: " + decrypted);
        }
    }
}
