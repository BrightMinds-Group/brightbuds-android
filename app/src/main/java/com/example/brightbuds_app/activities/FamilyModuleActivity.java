package com.example.brightbuds_app.activities;

import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.brightbuds_app.R;
import com.example.brightbuds_app.adapters.FamilyMembersAdapter;
import com.example.brightbuds_app.interfaces.DataCallbacks;
import com.example.brightbuds_app.models.FamilyMember;
import com.example.brightbuds_app.services.ProgressService;
import com.google.firebase.auth.FirebaseAuth;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * FamilyModuleActivity:
 * Displays local family album for the "My Family" module.
 *   - No Firebase image uploads/downloads.
 *   - Loads all family photos from the parent's local storage directory:
 *       /Android/data/com.example.brightbuds_app/files/MyFamily/
 *   - Maintains COPPA compliance (private, device-only images).
 */
public class FamilyModuleActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private static final String TAG = "FamilyModuleActivity";

    private RecyclerView recyclerView;
    private FamilyMembersAdapter adapter;
    private List<FamilyMember> familyMembers;
    private TextToSpeech textToSpeech;
    private String childId;
    private ProgressService progressService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_family_module);

        // Retrieve child ID (used for progress tracking)
        childId = getIntent().getStringExtra("childId");
        progressService = new ProgressService(this);

        initializeViews();
        setupTextToSpeech();

        // Load from local storage (no Firebase)
        loadLocalFamilyMembers();
    }

    // Sets up UI components and layout
    private void initializeViews() {
        ImageButton btnBack = findViewById(R.id.btnBack);
        TextView tvTitle = findViewById(R.id.tvTitle);
        TextView tvInstructions = findViewById(R.id.tvInstructions);

        btnBack.setOnClickListener(v -> finish());
        tvTitle.setText("Family Album");
        tvInstructions.setText("👆 Tap on a family member to hear their name!");

        recyclerView = findViewById(R.id.recyclerViewFamily);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
    }

    // Initializes text-to-speech engine
    private void setupTextToSpeech() {
        textToSpeech = new TextToSpeech(this, this);
    }

    /**
     * Loads all locally saved family photos and metadata.
     * Local directory: /Android/data/com.example.brightbuds_app/files/MyFamily/
     */
    private void loadLocalFamilyMembers() {
        familyMembers = new ArrayList<>();
        File dir = new File(getExternalFilesDir("MyFamily"), "");

        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();

            if (files != null && files.length > 0) {
                Log.d(TAG, "📂 Found " + files.length + " local family photos");
                for (File file : files) {
                    // Use filename as member name (without extension)
                    String name = file.getName().replaceFirst("[.][^.]+$", "");
                    FamilyMember member = new FamilyMember();
                    member.setName(name);
                    member.setLocalPath(file.getAbsolutePath());
                    member.setRelationship("Family Member");
                    familyMembers.add(member);
                }
            }
        }

        if (familyMembers.isEmpty()) {
            // Use placeholder members if no local photos found
            Log.d(TAG, "ℹ️ No local family photos found — loading defaults.");
            addDefaultFamilyMembers();
        }

        setupRecyclerView();
    }

    // Adds fallback default family members with drawable images
    private void addDefaultFamilyMembers() {
        familyMembers.add(new FamilyMember("Mom", "mother", R.drawable.default_mom, ""));
        familyMembers.add(new FamilyMember("Dad", "father", R.drawable.default_dad, ""));
        familyMembers.add(new FamilyMember("Grandma", "grandmother", R.drawable.default_grandma, ""));
        familyMembers.add(new FamilyMember("Grandpa", "grandfather", R.drawable.default_grandpa, ""));
        familyMembers.add(new FamilyMember("Sister", "sibling", R.drawable.default_sister, ""));
        familyMembers.add(new FamilyMember("Brother", "sibling", R.drawable.default_brother, ""));
    }

    // Sets up the RecyclerView adapter with click-to-speak functionality
    private void setupRecyclerView() {
        adapter = new FamilyMembersAdapter(familyMembers, member -> {
            // --- Speak the name aloud when tapped ---
            if (textToSpeech != null) {
                textToSpeech.speak(member.getName(), TextToSpeech.QUEUE_FLUSH, null, null);
                trackFamilyModuleProgress(member.getName());
            }

            // Optional toast + debug log
            Toast.makeText(this, "👂 " + member.getName(), Toast.LENGTH_SHORT).show();
            Log.d(TAG, "🔊 Spoken name: " + member.getName());
        });

        recyclerView.setAdapter(adapter);
    }

    // Records progress when a family member is tapped
    private void trackFamilyModuleProgress(String memberName) {
        if (childId == null) return;

        progressService.markModuleCompleted(childId, "family_module", 100,
                new DataCallbacks.GenericCallback() {
                    @Override
                    public void onSuccess(String result) {
                        Log.d(TAG, "✅ Progress tracked for: " + memberName);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "❌ Failed to track progress", e);
                    }
                });
    }

    // Text-to-Speech lifecycle methods
    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = textToSpeech.setLanguage(Locale.getDefault());
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "TTS language not supported");
            }
        } else {
            Log.e(TAG, "TTS initialization failed");
        }
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}
