package com.example.brightbuds_app.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.brightbuds_app.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * FamilyManagementActivity:
 * Allows parents to add new family members locally.
 *   - Photos are stored ONLY on the parent’s device (no Firebase Storage).
 *   - Firestore stores only text data and local file path.
 *   - Fully COPPA-compliant — no image upload to cloud.
 */
public class FamilyManagementActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 100;

    // UI elements
    private ImageView imgPreview;
    private EditText edtName, edtRelationship;
    private Button btnSave;

    // Local variables
    private Uri imageUri;
    private ProgressDialog progressDialog;

    // Firebase for metadata only (optional)
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_family_management);

        // Initialize UI
        imgPreview = findViewById(R.id.imgPreview);
        edtName = findViewById(R.id.edtName);
        edtRelationship = findViewById(R.id.edtRelationship);
        btnSave = findViewById(R.id.btnUpload);

        // Firebase (metadata only)
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Button actions
        imgPreview.setOnClickListener(v -> openFileChooser());
        btnSave.setOnClickListener(v -> saveFamilyMemberLocally());
    }

    // Opens gallery for parent to select a photo
    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK &&
                data != null && data.getData() != null) {
            imageUri = data.getData();
            Glide.with(this).load(imageUri).into(imgPreview);
        }
    }

    /**
     * Validates fields, saves image locally and writes metadata to Firestore.
     * No file ever leaves the device.
     */
    private void saveFamilyMemberLocally() {
        String name = edtName.getText().toString().trim();
        String relationship = edtRelationship.getText().toString().trim();

        if (name.isEmpty() || relationship.isEmpty() || imageUri == null) {
            Toast.makeText(this, "Please fill all fields and select an image", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog = ProgressDialog.show(this, "Saving", "Please wait...", true);

        try {
            // Step 1: Create app directory for MyFamily photos
            File dir = new File(getExternalFilesDir("MyFamily"), "");
            if (!dir.exists()) dir.mkdirs();

            // Step 2: Copy selected photo into app storage
            String fileName = name.replaceAll("\\s+", "_") + "_" + System.currentTimeMillis() + ".jpg";
            File localFile = new File(dir, fileName);

            try (InputStream in = getContentResolver().openInputStream(imageUri);
                 OutputStream out = new FileOutputStream(localFile)) {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = in.read(buffer)) > 0) out.write(buffer, 0, len);
            }

            // Step 3: Save metadata (no image upload)
            String parentId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "unknown_parent";

            Map<String, Object> data = new HashMap<>();
            data.put("name", name);
            data.put("relationship", relationship);
            data.put("localPath", localFile.getAbsolutePath()); // local-only file path
            data.put("parentId", parentId);
            data.put("createdAt", System.currentTimeMillis());

            db.collection("my_family")
                    .add(data)
                    .addOnSuccessListener(doc -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, "✅ Family member saved locally!", Toast.LENGTH_SHORT).show();
                        clearForm();
                    })
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, "⚠️ Saved locally, but Firestore failed: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });

        } catch (Exception e) {
            progressDialog.dismiss();
            Toast.makeText(this, "❌ Failed to save image: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    // Clears the form fields and resets the preview
    private void clearForm() {
        edtName.setText("");
        edtRelationship.setText("");
        imgPreview.setImageResource(R.drawable.ic_user_placeholder);
        imageUri = null;
    }
}
