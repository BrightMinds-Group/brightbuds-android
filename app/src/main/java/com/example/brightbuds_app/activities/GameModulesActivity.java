package com.example.brightbuds_app.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.example.brightbuds_app.R;
import com.example.brightbuds_app.models.Module;
import com.example.brightbuds_app.services.ModuleService;
import com.example.brightbuds_app.services.ProgressService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;

import java.util.List;

/**
 * Shows all learning modules for the selected child and launches
 * the correct Activity per module. Uses Firestore document ID to decide navigation.
 * Sends the metrics to Firebase for each game module only when
 * connected to WiFi
 */
public class GameModulesActivity extends AppCompatActivity {

    private LinearLayout container;
    private final ModuleService moduleService = new ModuleService();
    private ProgressService progressService;
    private String childId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_modules);

        container = findViewById(R.id.modulesContainer);
        progressService = new ProgressService(this);
        childId = getIntent().getStringExtra("childId");

        loadModules();
    }

    // Load all modules from Firestore
    private void loadModules() {
        container.removeAllViews();
        View loading = getLayoutInflater().inflate(R.layout.item_module_loading, container, false);
        container.addView(loading);

        moduleService.getAllModules(new ModuleService.ModulesCallback() {
            @Override
            public void onSuccess(List<Module> modules) {
                container.removeAllViews();
                if (modules == null || modules.isEmpty()) {
                    addEmpty("No modules available yet.");
                    return;
                }
                for (Module m : modules) {
                    if (m.isActive()) addModuleRow(m);
                }
            }

            @Override
            public void onError(Exception e) {
                container.removeAllViews();
                addEmpty("Failed to load modules: " + e.getMessage());
            }
        });
    }

    // Add one row (card) per module
    private void addModuleRow(Module module) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_module_row, container, false);

        MaterialTextView title = row.findViewById(R.id.txtModuleTitle);
        MaterialTextView subtitle = row.findViewById(R.id.txtModuleSubtitle);
        ImageView icon = row.findViewById(R.id.imgModuleIcon);
        MaterialButton btnPlay = row.findViewById(R.id.btnPlay);

        String displayTitle = module.getTitle() != null ? module.getTitle() : "Untitled Module";
        if ("Family Module".equalsIgnoreCase(displayTitle)) displayTitle = "My Family";
        title.setText(displayTitle);

        // Subtitle (lightweight defaults)
        String t = displayTitle.toLowerCase();
        if (t.contains("family"))       subtitle.setText("👨‍👩‍👧‍👦 Learn about family members!");
        else if (t.contains("monster")) subtitle.setText("🍬 Feed the Monster and learn words!");
        else if (t.contains("memory"))  subtitle.setText("🧠 Match and remember fun images!");
        else if (t.contains("match"))   subtitle.setText("🎮 Interactive Game");
        else if (t.contains("word"))    subtitle.setText("🔠 Build and learn new words!");
        else if (t.contains("abc"))     subtitle.setText("🎵 Sing along to the ABC Song!");
        else if (t.contains("123"))     subtitle.setText("🔢 Count and learn with numbers!");
        else                             subtitle.setText("🎮 Interactive learning module");

        // Icon
        icon.setImageResource(getModuleIcon(displayTitle));

        // Launch
        btnPlay.setOnClickListener(v -> launchModule(module));
        container.addView(row);
    }


    // Pick icon from title (simple & robust)
    private int getModuleIcon(String title) {
        if (title == null) return R.drawable.ic_module_generic;
        String t = title.toLowerCase();
        if (t.contains("family")) return R.drawable.ic_my_family;
        if (t.contains("monster")) return R.drawable.ic_monster;
        if (t.contains("memory")) return R.drawable.ic_memory;
        if (t.contains("match")) return R.drawable.ic_match;
        if (t.contains("word")) return R.drawable.ic_word_builder;
        if (t.contains("abc")) return R.drawable.ic_music;
        if (t.contains("123") || t.contains("number")) return R.drawable.ic_numbers;
        return R.drawable.ic_module_generic;
    }


// NAVIGATION — uses Firestore document ID
    private void launchModule(Module m) {
        String id = m.getId() != null ? m.getId().trim().toLowerCase() : "";
        String title = m.getTitle() != null ? m.getTitle().trim().toLowerCase() : "";
        String type  = m.getType()  != null ? m.getType().trim().toLowerCase()  : "";

        // DEBUG: Log what we're receiving
        System.out.println("DEBUG Module Clicked - ID: '" + id + "', Title: '" + title + "', Type: '" + type + "'");

        Intent i;

        // 1) Prefer stable IDs from Firestore
        switch (id) {
            case "family_module":
                i = new Intent(this, FamilyModuleActivity.class);
                System.out.println("DEBUG: Going to FamilyModuleActivity via ID match");
                break;

            case "game_feed_monster":
                i = new Intent(this, FeedMonsterActivity.class);
                System.out.println("DEBUG: Going to FeedMonsterActivity via ID match");
                break;

            case "game_match_letter":
                i = new Intent(this, MatchLetterActivity.class);
                System.out.println("DEBUG: Going to MatchLetterActivity via ID match");
                break;

            case "game_memory_match":
                i = new Intent(this, MemoryMatchActivity.class);
                System.out.println("DEBUG: Going to MemoryMatchActivity via ID match");
                break;

            case "game_word_builder":
                i = new Intent(this, WordBuilderActivity.class);
                System.out.println("DEBUG: Going to WordBuilderActivity via ID match");
                break;

            case "module_abc_song":
            case "module_123_song":
                i = new Intent(this, VideoModuleActivity.class);
                i.putExtra("storagePath", m.getStoragePath());
                System.out.println("DEBUG: Going to VideoModuleActivity via ID match");
                break;

            default:
                System.out.println("DEBUG: No ID match, falling back to title/type detection");
                // 2) Fallback to title/type if ID is missing or unexpected
                if (title.contains("family")) {
                    i = new Intent(this, FamilyModuleActivity.class);
                    System.out.println("DEBUG: Going to FamilyModuleActivity via title detection");
                } else if (title.contains("monster")) {
                    i = new Intent(this, FeedMonsterActivity.class);
                    System.out.println("DEBUG: Going to FeedMonsterActivity via title detection");
                } else if (title.contains("match") && title.contains("letter")) {
                    i = new Intent(this, MatchLetterActivity.class);
                    System.out.println("DEBUG: Going to MatchLetterActivity via title detection");
                } else if (title.contains("memory") || title.contains("match")) {
                    i = new Intent(this, MemoryMatchActivity.class);
                    System.out.println("DEBUG: Going to MemoryMatchActivity via title detection");
                } else if (title.contains("word") || title.contains("builder")) {
                    i = new Intent(this, WordBuilderActivity.class);
                    System.out.println("DEBUG: Going to WordBuilderActivity via title detection");
                } else if ("video".equals(type) || title.contains("abc") || title.contains("123")) {
                    i = new Intent(this, VideoModuleActivity.class);
                    i.putExtra("storagePath", m.getStoragePath());
                    System.out.println("DEBUG: Going to VideoModuleActivity via type/title detection");
                } else {
                    i = new Intent(this, ComingSoonActivity.class);
                    i.putExtra("message", "New learning module coming soon!");
                    System.out.println("DEBUG: Going to ComingSoonActivity - no matches found");
                }
                break;
        }

        i.putExtra("childId", childId);
        i.putExtra("moduleId", m.getId());
        i.putExtra("moduleTitle", m.getTitle());
        startActivity(i);
    }

    // Add empty row (card)
    private void addEmpty(String message) {
        View empty = LayoutInflater.from(this).inflate(R.layout.item_module_empty, container, false);
        ((MaterialTextView) empty.findViewById(R.id.txtEmpty)).setText(message);
        container.addView(empty);
    }

}
