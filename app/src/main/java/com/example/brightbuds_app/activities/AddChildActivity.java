package com.example.brightbuds_app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.brightbuds_app.R;
import com.example.brightbuds_app.interfaces.DataCallbacks;
import com.example.brightbuds_app.models.ChildProfile;
import com.example.brightbuds_app.services.ChildProfileService;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * AddChildActivity - Working version that matches the layout (no age field)
 */
public class AddChildActivity extends AppCompatActivity {

    private TextInputEditText editTextChildName;
    private MaterialAutoCompleteTextView spinnerGender, spinnerLearningLevel;
    private MaterialButton buttonSaveChild;
    private MaterialToolbar toolbar;

    private ChildProfileService childProfileService;
    private FirebaseAuth auth;
    private static final String TAG = "AddChildActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_profile);

        childProfileService = new ChildProfileService();
        auth = FirebaseAuth.getInstance();

        initializeViews();
        setupDropdowns();
        setupToolbar();
        setupListeners();

        Log.d(TAG, "✅ AddChildActivity started successfully");
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar_add_child);
        editTextChildName = findViewById(R.id.editTextChildName);
        spinnerGender = findViewById(R.id.spinnerGender);
        spinnerLearningLevel = findViewById(R.id.spinnerLearningLevel);
        buttonSaveChild = findViewById(R.id.buttonSaveChild);

        Log.d(TAG, "🔍 View initialization:");
        Log.d(TAG, "editTextChildName: " + (editTextChildName != null ? "FOUND" : "NULL"));
        Log.d(TAG, "spinnerGender: " + (spinnerGender != null ? "FOUND" : "NULL"));
        Log.d(TAG, "spinnerLearningLevel: " + (spinnerLearningLevel != null ? "FOUND" : "NULL"));
        Log.d(TAG, "buttonSaveChild: " + (buttonSaveChild != null ? "FOUND" : "NULL"));
    }

    private void setupDropdowns() {
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

    private void setupToolbar() {
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupListeners() {
        buttonSaveChild.setOnClickListener(v -> {
            Log.d(TAG, "🎯 Save button clicked");

            String name = editTextChildName.getText() != null ? editTextChildName.getText().toString().trim() : "";
            String gender = spinnerGender.getText().toString().trim();
            String level = spinnerLearningLevel.getText().toString().trim();

            Log.d(TAG, "📝 Input values - Name: '" + name + "', Gender: '" + gender + "', Level: '" + level + "'");

            if (!validateInputs(name, gender, level)) return;

            FirebaseUser currentUser = auth.getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(this, "⚠️ Please log in first", Toast.LENGTH_SHORT).show();
                return;
            }

            String parentId = currentUser.getUid();

            // Create child with age 0 (no age field in layout)
            ChildProfile newChild = new ChildProfile(parentId, name, 0, gender, level);

            buttonSaveChild.setEnabled(false);
            buttonSaveChild.setText("Saving...");

            childProfileService.saveChildProfile(newChild, new DataCallbacks.GenericCallback() {
                @Override
                public void onSuccess(String childId) {
                    runOnUiThread(() -> {
                        Log.i(TAG, "🎉 SUCCESS: Child saved with ID: " + childId);
                        Toast.makeText(AddChildActivity.this,
                                "✅ Child profile added successfully!",
                                Toast.LENGTH_SHORT).show();

                        buttonSaveChild.setEnabled(true);
                        buttonSaveChild.setText("Save Child Profile");

                        Intent intent = new Intent(AddChildActivity.this, RoleSelectionActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                        finish();
                    });
                }

                @Override
                public void onFailure(Exception e) {
                    runOnUiThread(() -> {
                        Log.e(TAG, "💥 SAVE FAILED: " + e.getMessage());
                        Toast.makeText(AddChildActivity.this,
                                "❌ Failed to save child: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();

                        buttonSaveChild.setEnabled(true);
                        buttonSaveChild.setText("Save Child Profile");
                    });
                }
            });
        });
    }

    private boolean validateInputs(String name, String gender, String level) {
        if (TextUtils.isEmpty(name)) {
            editTextChildName.setError("Enter child's name");
            return false;
        }
        if (TextUtils.isEmpty(gender) || gender.equals("Select Gender")) {
            spinnerGender.setError("Select gender");
            return false;
        }
        if (TextUtils.isEmpty(level) || level.equals("Select Level")) {
            spinnerLearningLevel.setError("Select learning level");
            return false;
        }
        return true;
    }
}