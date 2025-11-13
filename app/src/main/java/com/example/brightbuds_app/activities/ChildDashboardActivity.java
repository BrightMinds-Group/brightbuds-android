package com.example.brightbuds_app.activities;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.brightbuds_app.R;
import com.example.brightbuds_app.models.Module;
import com.example.brightbuds_app.services.ModuleService;
import com.example.brightbuds_app.services.StorageService;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class ChildDashboardActivity extends AppCompatActivity {

    private GridLayout moduleGrid;
    private final ModuleService moduleService = new ModuleService();
    private String childId;
    private boolean isParentMode = false;

    private static final String TAG = "ChildDashboardActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_dashboard);

        moduleGrid = findViewById(R.id.moduleGrid);

        // 1. Try get childId from Intent
        childId = getIntent().getStringExtra("childId");

        // 2. If missing, fall back to stored selection
        if (childId == null || childId.isEmpty()) {
            var prefs = getSharedPreferences("BrightBudsPrefs", Context.MODE_PRIVATE);
            childId = prefs.getString("selectedChildId", null);
        }

        // 3. Only if still missing, show the toast and exit
        if (childId == null || childId.isEmpty()) {
            Log.w(TAG, "No childId available for ChildDashboardActivity");
            Toast.makeText(this,
                    "Child profile not found. Please reselect a child.",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Check if parent is logged in (unlocks all modules)
        isParentMode = FirebaseAuth.getInstance().getCurrentUser() != null;
        Log.d(TAG, "Parent Mode: " + isParentMode);

        loadModules();
    }

    /** Load all active modules */
    private void loadModules() {
        moduleGrid.removeAllViews();

        moduleService.getAllModules(new ModuleService.ModulesCallback() {
            @Override
            public void onSuccess(List<Module> modules) {
                if (modules == null || modules.isEmpty()) {
                    Log.w(TAG, "No modules returned from service");
                    Toast.makeText(ChildDashboardActivity.this,
                            "No modules available yet.", Toast.LENGTH_SHORT).show();
                    return;
                }

                Log.d(TAG, "Modules loaded: " + modules.size());

                for (Module module : modules) {
                    if (module.isActive()) {
                        addModuleIcon(module);
                    } else {
                        Log.d(TAG, "Skipped inactive module: " + module.getTitle());
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Failed to load modules", e);
                Toast.makeText(ChildDashboardActivity.this,
                        "Failed to load modules: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    /** Create and add a module tile to the grid */
    private void addModuleIcon(Module module) {
        View tile = LayoutInflater.from(this)
                .inflate(R.layout.item_child_module_icon, moduleGrid, false);

        ImageView icon = tile.findViewById(R.id.imgModuleIcon);
        ImageView lockIcon = tile.findViewById(R.id.imgLockIcon);

        TextView title = tile.findViewById(R.id.txtModuleTitle);
        if (title != null) {
            title.setText(module.getTitle() != null ? module.getTitle() : "Untitled Module");
        }

        // Check if module is locked
        boolean isLocked = isModuleLocked(module);
        Log.d(TAG, "Module: " + module.getId() + " Locked: " + isLocked);

        if (isLocked) {
            icon.setAlpha(0.4f);
            lockIcon.setVisibility(View.VISIBLE);
            tile.setEnabled(false);
        } else {
            icon.setAlpha(1.0f);
            lockIcon.setVisibility(View.GONE);
            tile.setEnabled(true);
        }

        // Load icon (either from Storage or drawable)
        if (module.getIcon() != null && module.getIcon().startsWith("modules/")) {
            StorageService.getInstance().getOrDownloadFile(
                    this,
                    module.getIcon(),
                    uri -> runOnUiThread(() -> icon.setImageURI(uri)),
                    e -> icon.setImageResource(R.drawable.ic_module_generic)
            );
        } else {
            int resId = getDrawableIdByName(this, module.getIcon());
            icon.setImageResource(resId != 0 ? resId : R.drawable.ic_module_generic);
        }

        // Handle clicks
        if (!isLocked) {
            tile.setOnClickListener(v -> openModule(module));
        } else {
            tile.setOnClickListener(v -> showLockedMessage(module));
        }

        moduleGrid.addView(tile);
    }

    /** Determine locked/unlocked state */
    private boolean isModuleLocked(Module module) {
        if (module == null || module.getId() == null) return true;

        // Always unlocked modules
        if (module.getId().equals("module_abc_song") ||
                module.getId().equals("module_123_song")) {
            return false;
        }

        // All others locked unless parent is signed in
        return !isParentMode;
    }

    /** Show toast if locked */
    private void showLockedMessage(Module module) {
        String title = module.getTitle() != null ? module.getTitle() : "This module";
        Toast.makeText(this,
                "🔒 " + title + " is locked. Parent login required to unlock.",
                Toast.LENGTH_LONG).show();
    }

    /** Navigate to the correct screen based on module ID */
    private void openModule(Module module) {
        Intent intent;
        String title = module.getTitle() != null ? module.getTitle().toLowerCase() : "";
        String type = module.getType() != null ? module.getType() : "video";
        String moduleId = module.getId() != null ? module.getId().toLowerCase() : "";

        switch (moduleId) {
            case "family_module":
                intent = new Intent(this, FamilyModuleActivity.class);
                intent.putExtra("moduleTitle", "My Family");
                break;

            case "game_feed_monster":
                intent = new Intent(this, FeedMonsterActivity.class);
                break;

            case "game_match_letter":
                intent = new Intent(this, MatchLetterActivity.class);
                break;

            case "game_memory_match":
                intent = new Intent(this, MemoryMatchActivity.class);
                break;

            case "game_word_builder":
                intent = new Intent(this, WordBuilderActivity.class);
                break;

            case "module_abc_song":
            case "module_123_song":
                intent = new Intent(this, VideoModuleActivity.class);
                intent.putExtra("storagePath", module.getStoragePath());
                break;

            default:
                if (title.contains("family") && !title.contains("words")) {
                    intent = new Intent(this, FamilyModuleActivity.class);
                } else if (title.contains("monster")) {
                    intent = new Intent(this, FeedMonsterActivity.class);
                } else if (title.contains("match") && title.contains("letter")) {
                    intent = new Intent(this, MatchLetterActivity.class);
                } else if (title.contains("memory")
                        || (title.contains("match") && !title.contains("letter"))) {
                    intent = new Intent(this, MemoryMatchActivity.class);
                } else if (title.contains("word") || title.contains("builder")) {
                    intent = new Intent(this, WordBuilderActivity.class);
                } else if (type.equalsIgnoreCase("video")
                        || title.contains("song")
                        || title.contains("abc")
                        || title.contains("123")) {
                    intent = new Intent(this, VideoModuleActivity.class);
                    intent.putExtra("storagePath", module.getStoragePath());
                } else {
                    intent = new Intent(this, ComingSoonActivity.class);
                    intent.putExtra("message",
                            module.getTitle() + " is coming soon!");
                }
                break;
        }

        intent.putExtra("childId", childId);
        intent.putExtra("moduleId", module.getId());
        intent.putExtra("moduleTitle", module.getTitle());
        startActivity(intent);
    }

    /** Get drawable resource by name safely */
    private static int getDrawableIdByName(Context ctx, String name) {
        if (name == null || name.isEmpty()) return 0;
        return ctx.getResources().getIdentifier(name, "drawable", ctx.getPackageName());
    }
}
