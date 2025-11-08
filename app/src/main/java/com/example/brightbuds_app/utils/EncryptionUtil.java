package com.example.brightbuds_app.utils;

import android.util.Base64;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * EncryptionUtil
 * -----------------------
 * Provides AES-256 encryption and decryption for sensitive fields like name, gender, and displayName.
 * Uses PBKDF2 for key derivation and random IV for strong encryption.
 */
public class EncryptionUtil {

    private static final String TAG = "EncryptionUtil";
    private static final String SECRET_KEY = "brightbuds_secure_key"; // never expose real key
    private static final String SALT = "brightbuds_salt_value";
    private static final String AES_MODE = "AES/CBC/PKCS5Padding";

    /** Derive AES key from passphrase */
    private static SecretKeySpec generateKey() throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        PBEKeySpec spec = new PBEKeySpec(SECRET_KEY.toCharArray(), SALT.getBytes(StandardCharsets.UTF_8), 65536, 256);
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), "AES");
    }

    /** Encrypt text with AES-256 + random IV */
    public static String encrypt(String data) {
        if (data == null) return "";
        try {
            Cipher cipher = Cipher.getInstance(AES_MODE);
            SecretKeySpec key = generateKey();

            byte[] iv = new byte[16];
            new SecureRandom().nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
            byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

            return Base64.encodeToString(combined, Base64.DEFAULT);
        } catch (Exception e) {
            Log.e(TAG, "❌ Encryption failed: " + e.getMessage());
            return ""; // Return empty instead of null to avoid crashing Firestore writes
        }
    }

    /** Decrypt Base64 AES-256 text */
    public static String decrypt(String base64Data) {
        if (base64Data == null || base64Data.trim().isEmpty()) return "";
        try {
            byte[] combined = Base64.decode(base64Data, Base64.DEFAULT);
            if (combined.length < 17) return "";

            byte[] iv = new byte[16];
            byte[] encrypted = new byte[combined.length - 16];
            System.arraycopy(combined, 0, iv, 0, 16);
            System.arraycopy(combined, 16, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance(AES_MODE);
            SecretKeySpec key = generateKey();
            cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));

            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            Log.e(TAG, "❌ Decryption failed: " + e.getMessage());
            return "";
        }
    }
}
