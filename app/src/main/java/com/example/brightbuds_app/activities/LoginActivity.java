package com.example.brightbuds_app.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.brightbuds_app.R;
import com.example.brightbuds_app.services.AuthServices;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Handles login, email verification enforcement,
 * and password reset.
 */
public class LoginActivity extends AppCompatActivity {

    private TextInputEditText emailEditText, passwordEditText;
    private MaterialButton loginButton;
    private TextView registerRedirect, forgotPasswordText;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private AuthServices authServices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        authServices = new AuthServices(this);

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        registerRedirect = findViewById(R.id.registerRedirect);
        forgotPasswordText = findViewById(R.id.forgotPassword);

        loginButton.setOnClickListener(v -> attemptLogin());
        registerRedirect.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
        forgotPasswordText.setOnClickListener(v -> showForgotPasswordDialog());
    }

    private void attemptLogin() {
        String email = val(emailEditText);
        String pass = val(passwordEditText);

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Enter a valid email");
            return;
        }
        if (TextUtils.isEmpty(pass)) {
            passwordEditText.setError("Enter password");
            return;
        }

        loginButton.setEnabled(false);
        loginButton.setText("Signing in…");

        auth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = auth.getCurrentUser();
                    if (user != null) handleLoginSuccess(user);
                })
                .addOnFailureListener(e -> {
                    loginButton.setEnabled(true);
                    loginButton.setText("Login");
                    Toast.makeText(this, "Login failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /** Handles post-login behavior */
    private void handleLoginSuccess(FirebaseUser user) {
        if (user.isEmailVerified()) {
            Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, RoleSelectionActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else {
            showEmailVerificationDialog(user);
        }
    }

    /** Dialog shown for unverified users */
    private void showEmailVerificationDialog(FirebaseUser user) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Email Verification Required")
                .setMessage("Please verify your email before logging in.\n\nA verification link has been sent to " + user.getEmail())
                .setPositiveButton("Resend Verification", (dialog, which) -> {
                    user.sendEmailVerification()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(this, "Verification email sent again!", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(this, "Failed to resend verification email.", Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("OK", (dialog, which) -> {
                    auth.signOut(); // Prevent access without verification
                    dialog.dismiss();
                })
                .show();
    }

    /** Forgot-password dialog */
    private void showForgotPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reset Password");

        final EditText input = new EditText(this);
        input.setHint("Enter your registered email");
        input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        input.setPadding(60, 40, 60, 40);
        builder.setView(input);

        builder.setPositiveButton("Send Reset Link", (dialog, which) -> {
            String email = input.getText().toString().trim();
            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                    .addOnSuccessListener(aVoid -> Toast.makeText(this,
                            "📩 Password reset link sent to " + email,
                            Toast.LENGTH_LONG).show())
                    .addOnFailureListener(e -> Toast.makeText(this,
                            "Error: " + e.getMessage(),
                            Toast.LENGTH_LONG).show());
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    private String val(TextInputEditText t) {
        return t.getText() == null ? "" : t.getText().toString().trim();
    }
}
