package com.example.brightbuds_app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.brightbuds_app.R;
import com.example.brightbuds_app.interfaces.DataCallbacks;
import com.example.brightbuds_app.services.AuthServices;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles user registration with FirebaseAuth and Firestore.
 * Includes email verification enforcement.
 */
public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText fullNameEditText, emailEditText, passwordEditText, confirmPasswordEditText;
    private RadioGroup roleGroup;
    private Button registerButton;
    private TextView loginRedirect;

    private AuthServices authService;
    private FirebaseFirestore db;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        authService = new AuthServices(this);
        db = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        fullNameEditText = findViewById(R.id.fullNameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        registerButton = findViewById(R.id.registerButton);
        loginRedirect = findViewById(R.id.loginRedirect);

        registerButton.setOnClickListener(v -> attemptRegistration());
        loginRedirect.setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class)));
    }

    private void attemptRegistration() {
        String fullName = safeText(fullNameEditText);
        String email = safeText(emailEditText);
        String password = safeText(passwordEditText);
        String confirm = safeText(confirmPasswordEditText);

        if (!isFormValid(fullName, email, password, confirm)) return;

        final String userType = "parent";

        registerButton.setEnabled(false);
        registerButton.setText("Creating Account…");

        authService.registerUser(email, password, fullName, new DataCallbacks.GenericCallback() {
            @Override
            public void onSuccess(String result) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user == null) {
                    onFailure(new Exception("User not available after registration."));
                    return;
                }

                // Send verification email
                user.sendEmailVerification()
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(RegisterActivity.this,
                                    "📩 Verification link sent! Please check your email.",
                                    Toast.LENGTH_LONG).show();
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(RegisterActivity.this,
                                        "Failed to send verification email: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show());

                String uid = user.getUid();

                // Save minimal user info
                Map<String, Object> data = new HashMap<>();
                data.put("uid", uid);
                data.put("fullName", fullName);
                data.put("email", email);
                data.put("type", userType);
                data.put("createdAt", System.currentTimeMillis());
                data.put("emailVerified", false);

                db.collection("users").document(uid)
                        .set(data)
                        .addOnSuccessListener(unused -> {
                            firebaseAuth.signOut(); // Force verification before login
                            registerButton.setEnabled(true);
                            registerButton.setText("Create Account");

                            Toast.makeText(RegisterActivity.this,
                                    "Account created! Please verify your email before logging in.",
                                    Toast.LENGTH_LONG).show();

                            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            registerButton.setEnabled(true);
                            registerButton.setText("Create Account");
                            Toast.makeText(RegisterActivity.this,
                                    "Saving profile failed: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        });
            }

            @Override
            public void onFailure(Exception e) {
                registerButton.setEnabled(true);
                registerButton.setText("Create Account");
                Toast.makeText(RegisterActivity.this,
                        "Registration failed: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private String safeText(TextInputEditText t) {
        return t.getText() == null ? "" : t.getText().toString().trim();
    }

    private boolean isFormValid(String name, String email, String password, String confirm) {
        if (TextUtils.isEmpty(name)) {
            fullNameEditText.setError("Full name is required.");
            return false;
        }
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Enter a valid email.");
            return false;
        }
        if (password.length() < 6) {
            passwordEditText.setError("Password must be at least 6 characters.");
            return false;
        }
        if (!password.equals(confirm)) {
            confirmPasswordEditText.setError("Passwords do not match.");
            return false;
        }
        return true;
    }
}
