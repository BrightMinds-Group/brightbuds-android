package com.example.brightbuds_app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.brightbuds_app.R;
import com.example.brightbuds_app.interfaces.DataCallbacks;
import com.example.brightbuds_app.models.ChildProfile;
import com.example.brightbuds_app.services.ChildProfileService;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


public class ChildProfileActivity extends AppCompatActivity {

    private EditText editTextChildName, editTextChildAge;
    private AutoCompleteTextView spinnerGender, spinnerLearningLevel;
    private Button buttonSaveChild;

    private ChildProfileService childProfileService;
    private FirebaseAuth auth;
    private static final String TAG = "ChildProfileActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_profile);

        childProfileService = new ChildProfileService();
        auth = FirebaseAuth.getInstance();

        initializeViews();
        setupToolbar();
        setupDropdowns();
        setupSaveButton();

        Log.d(TAG, "✅ ChildProfileActivity started successfully");
    }

    /** Initialize UI components with null checks */
    private void initializeViews() {
        editTextChildName = findViewById(R.id.editTextChildName);
        editTextChildAge = findViewById(R.id.editTextChildAge);
        spinnerGender = findViewById(R.id.spinnerGender);
        spinnerLearningLevel = findViewById(R.id.spinnerLearningLevel);
        buttonSaveChild = findViewById(R.id.buttonSaveChild);

        // DEBUG: Check if views are properly found
        Log.d(TAG, "🔍 View initialization:");
        Log.d(TAG, "editTextChildName: " + (editTextChildName != null ? "FOUND" : "NULL"));
        Log.d(TAG, "editTextChildAge: " + (editTextChildAge != null ? "FOUND" : "NULL"));
        Log.d(TAG, "spinnerGender: " + (spinnerGender != null ? "FOUND" : "NULL"));
        Log.d(TAG, "spinnerLearningLevel: " + (spinnerLearningLevel != null ? "FOUND" : "NULL"));
        Log.d(TAG, "buttonSaveChild: " + (buttonSaveChild != null ? "FOUND" : "NULL"));

        if (editTextChildName == null || editTextChildAge == null) {
            Log.e(TAG, "❌ CRITICAL: EditText views are null - check layout XML IDs");
            Toast.makeText(this, "App configuration error. Please restart.", Toast.LENGTH_LONG).show();
        }
    }

    /** Configure toolbar navigation */
    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar_add_child);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    /** Populate dropdown lists */
    private void setupDropdowns() {
        if (spinnerGender == null || spinnerLearningLevel == null) {
            Log.e(TAG, "❌ Spinners are null - cannot setup dropdowns");
            return;
        }

        ArrayAdapter<CharSequence> genderAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.gender_options,
                android.R.layout.simple_dropdown_item_1line
        );
        spinnerGender.setAdapter(genderAdapter);

        ArrayAdapter<CharSequence> levelAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.learning_levels,
                android.R.layout.simple_dropdown_item_1line
        );
        spinnerLearningLevel.setAdapter(levelAdapter);
    }

    /** Validate form and save to Firestore with null checks */
    private void setupSaveButton() {
        if (buttonSaveChild == null) {
            Log.e(TAG, "❌ Save button is null - cannot setup click listener");
            return;
        }

        buttonSaveChild.setOnClickListener(v -> {
            Log.d(TAG, "🎯 Save button clicked");

            // Add null checks for EditText fields
            if (editTextChildName == null || editTextChildAge == null) {
                Log.e(TAG, "❌ EditText fields are null - cannot get input");
                Toast.makeText(this, "App error: Form fields not loaded", Toast.LENGTH_LONG).show();
                return;
            }

            String name = editTextChildName.getText() != null ? editTextChildName.getText().toString().trim() : "";
            String ageStr = editTextChildAge.getText() != null ? editTextChildAge.getText().toString().trim() : "";
            String gender = spinnerGender != null ? spinnerGender.getText().toString().trim() : "";
            String learningLevel = spinnerLearningLevel != null ? spinnerLearningLevel.getText().toString().trim() : "";

            Log.d(TAG, "📝 Input values - Name: '" + name + "', Age: '" + ageStr + "', Gender: '" + gender + "', Level: '" + learningLevel + "'");

            if (!validateInputs(name, ageStr, gender, learningLevel)) {
                Log.e(TAG, "❌ Input validation failed");
                return;
            }

            int age;
            try {
                age = Integer.parseInt(ageStr);
            } catch (NumberFormatException e) {
                Log.e(TAG, "❌ Age parsing failed: " + ageStr);
                editTextChildAge.setError("Invalid age format");
                return;
            }

            FirebaseUser currentUser = auth.getCurrentUser();
            if (currentUser == null) {
                Log.e(TAG, "❌ No user logged in");
                Toast.makeText(this, "⚠️ Please log in first", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            String parentId = currentUser.getUid();
            Log.d(TAG, "👤 Parent ID: " + parentId);

            // Create child profile
            ChildProfile newChild = new ChildProfile(parentId, name, age, gender, learningLevel);
            newChild.setDisplayName(name);
            newChild.setActive(true);

            // Log data before save
            Log.d(TAG, "🎯 Creating child profile:");
            Log.d(TAG, "Name: " + name);
            Log.d(TAG, "Age: " + age);
            Log.d(TAG, "Gender: " + gender);
            Log.d(TAG, "Level: " + learningLevel);
            Log.d(TAG, "ParentId: " + parentId);

            buttonSaveChild.setEnabled(false);
            buttonSaveChild.setText("Saving...");

            childProfileService.saveChildProfile(newChild, new DataCallbacks.GenericCallback() {
                @Override
                public void onSuccess(String childId) {
                    Log.i(TAG, "✅ Child saved successfully! ID: " + childId);
                    runOnUiThread(() -> {
                        Toast.makeText(ChildProfileActivity.this,
                                "✅ Child profile saved securely!",
                                Toast.LENGTH_SHORT).show();

                        buttonSaveChild.setEnabled(true);
                        buttonSaveChild.setText("Save Child Profile");

                        Intent intent = new Intent(ChildProfileActivity.this, RoleSelectionActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                        finish();
                    });
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "❌ Save failed: " + e.getMessage());
                    runOnUiThread(() -> {
                        Toast.makeText(ChildProfileActivity.this,
                                "❌ Failed to save child: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                        buttonSaveChild.setEnabled(true);
                        buttonSaveChild.setText("Save Child Profile");
                    });
                }
            });
        });
    }

    /** Validate all form fields */
    private boolean validateInputs(String name, String ageStr, String gender, String level) {
        if (TextUtils.isEmpty(name)) {
            if (editTextChildName != null) {
                editTextChildName.setError("Enter your child's name");
            }
            return false;
        }

        if (name.length() < 2) {
            if (editTextChildName != null) {
                editTextChildName.setError("Name must be at least 2 characters");
            }
            return false;
        }

        if (TextUtils.isEmpty(ageStr)) {
            if (editTextChildAge != null) {
                editTextChildAge.setError("Enter your child's age");
            }
            return false;
        }

        try {
            int age = Integer.parseInt(ageStr);
            if (age < 1 || age > 10) {
                if (editTextChildAge != null) {
                    editTextChildAge.setError("Age must be between 1 and 10");
                }
                return false;
            }
        } catch (NumberFormatException e) {
            if (editTextChildAge != null) {
                editTextChildAge.setError("Invalid age format");
            }
            return false;
        }

        if (TextUtils.isEmpty(gender) || gender.equals("Select Gender")) {
            if (spinnerGender != null) {
                spinnerGender.setError("Select gender");
            }
            return false;
        }

        if (TextUtils.isEmpty(level) || level.equals("Select Level")) {
            if (spinnerLearningLevel != null) {
                spinnerLearningLevel.setError("Select learning level");
            }
            return false;
        }

        Log.d(TAG, "✅ All inputs validated successfully");
        return true;
    }
}