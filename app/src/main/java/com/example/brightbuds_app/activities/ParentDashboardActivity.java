package com.example.brightbuds_app.activities;

import static com.example.brightbuds_app.R.*;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.example.brightbuds_app.R;
import com.example.brightbuds_app.interfaces.DataCallbacks;
import com.example.brightbuds_app.interfaces.ProgressListCallback;
import com.example.brightbuds_app.models.ChildProfile;
import com.example.brightbuds_app.models.Progress;
import com.example.brightbuds_app.services.AuthServices;
import com.example.brightbuds_app.services.ChildProfileService;
import com.example.brightbuds_app.services.ProgressService;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParentDashboardActivity extends AppCompatActivity {

    private static final String TAG = "ParentDashboard";

    // UI components
    private TextView txtWelcome;
    private LinearLayout childrenContainer;

    // Firebase + services
    private String parentId;
    private AuthServices authService;
    private ChildProfileService childService;
    private ProgressService progressService;

    // Safety flag to prevent duplicate loading
    private boolean isLoadingChildren = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_dashboard);

        // Check if user is logged in
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Get parent ID
        parentId = currentUser.getUid();

        // Initialize services
        authService = new AuthServices(this);
        childService = new ChildProfileService();
        progressService = new ProgressService(this);

        txtWelcome = findViewById(R.id.txtWelcomeParent);
        childrenContainer = findViewById(R.id.childrenContainer);

        String parentName = currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "Parent";
        txtWelcome.setText("Welcome back " + parentName + "!");

        // Manage Family button
        MaterialButton btnManageFamily = findViewById(R.id.btnManageFamily);
        btnManageFamily.setOnClickListener(v ->
                startActivity(new Intent(this, FamilyManagementActivity.class)));

        // Bottom navigation bar
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnItemSelectedListener(this::onBottomNavSelected);

        Log.d(TAG, "✅ ParentDashboard loaded for parent: " + parentId);
    }

    // Handle bottom navigation clicks
    private boolean onBottomNavSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            Intent intent = new Intent(this, RoleSelectionActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
            return true;
        } else if (id == R.id.nav_add_child) {
            startActivity(new Intent(this, ChildProfileActivity.class));
            return true;
        } else if (id == R.id.nav_reports) {
            SharedPreferences prefs = getSharedPreferences("BrightBudsPrefs", MODE_PRIVATE);
            String selectedChildId = prefs.getString("selectedChildId", null);
            Intent i = new Intent(this, ReportGenerationActivity.class);
            if (selectedChildId != null) i.putExtra("childId", selectedChildId);
            startActivity(i);
            return true;
        } else if (id == R.id.nav_profile) {
            startActivity(new Intent(this, ParentProfileActivity.class));
            return true;
        }
        return false;
    }

    // Load children and their progress with safety flag
    private void loadChildrenAndProgress() {
        // Prevent duplicate calls
        if (isLoadingChildren) {
            Log.d(TAG, "⏳ Skipping duplicate loadChildrenAndProgress call");
            return;
        }
        isLoadingChildren = true;

        childrenContainer.removeAllViews();

        // Show loading spinner while fetching data
        View loadingView = getLayoutInflater().inflate(R.layout.item_loading_children, childrenContainer, false);
        childrenContainer.addView(loadingView);

        Log.d(TAG, "🔄 Loading children and progress data...");

        // Get children for logged-in parent
        childService.getChildrenForCurrentParent(new DataCallbacks.ChildrenListCallback() {
            @Override
            public void onSuccess(List<ChildProfile> children) {
                childrenContainer.removeAllViews();

                Log.d(TAG, "✅ Loaded " + children.size() + " children");

                if (children.isEmpty()) {
                    // Show empty state
                    View emptyView = getLayoutInflater().inflate(R.layout.item_empty_children, childrenContainer, false);
                    childrenContainer.addView(emptyView);
                    loadModuleOverviewChart(new ArrayList<>());
                    isLoadingChildren = false;
                    return;
                }

                // Collect all child IDs
                List<String> childIds = new ArrayList<>();
                for (ChildProfile child : children) childIds.add(child.getChildId());

                // Get progress data for these children
                progressService.getAllProgressForParentWithChildren(parentId, childIds, new ProgressListCallback() {
                    @Override
                    public void onSuccess(List<Progress> progressList) {
                        Log.d(TAG, "✅ Loaded " + progressList.size() + " progress records");

                        // Display child cards
                        for (ChildProfile child : children) {
                            childrenContainer.addView(createChildCard(child, progressList));
                        }
                        // Load bar graph for module overview
                        loadModuleOverviewChart(progressList);
                        isLoadingChildren = false;
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "❌ Failed to load progress", e);
                        // Still show children without progress
                        for (ChildProfile child : children) {
                            childrenContainer.addView(createChildCard(child, new ArrayList<>()));
                        }
                        loadModuleOverviewChart(new ArrayList<>());
                        isLoadingChildren = false;
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                childrenContainer.removeAllViews();
                Log.e(TAG, "❌ Failed to load children", e);

                // Show error state
                View errorView = getLayoutInflater().inflate(R.layout.item_error_children, childrenContainer, false);
                childrenContainer.addView(errorView);
                loadModuleOverviewChart(new ArrayList<>());
                isLoadingChildren = false;
            }
        });
    }

    // Module Overview Bar Chart
    private void loadModuleOverviewChart(List<Progress> progressList) {
        BarChart chart = findViewById(R.id.moduleOverviewChart);
        if (chart == null) return;

        // All modules with default types
        Map<String, String> moduleTypes = new HashMap<>();
        moduleTypes.put("module_123_song", "video");
        moduleTypes.put("module_abc_song", "video");
        moduleTypes.put("module_feed_the_monster", "game");
        moduleTypes.put("module_match_the_letter", "game");
        moduleTypes.put("module_memory_match", "game");
        moduleTypes.put("module_word_builder", "game");
        moduleTypes.put("module_my_family", "game");

        // Track module values
        Map<String, Integer> moduleValues = new HashMap<>();
        for (String module : moduleTypes.keySet()) moduleValues.put(module, 0);

        Log.d(TAG, "🎯 Progress list size: " + progressList.size());

        // Go through all progress records
        for (Progress progress : progressList) {
            String moduleId = progress.getModuleId();
            if (moduleId == null || !moduleTypes.containsKey(moduleId)) continue;

            // Prefer Firestore 'type' field if present
            String type = progress.getType() != null ? progress.getType() : moduleTypes.get(moduleId);

            if ("video".equals(type)) {
                int plays = progress.getPlays();
                moduleValues.put(moduleId, moduleValues.get(moduleId) + (plays > 0 ? plays : 1));
            } else {
                int score = (int) Math.round(progress.getScore());
                moduleValues.put(moduleId, Math.max(moduleValues.get(moduleId), score));
            }

            Log.d(TAG, "📘 Module=" + moduleId + " | Type=" + type + " | Plays=" + progress.getPlays() + " | Score=" + progress.getScore());
        }

        // Prepare chart entries
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int index = 0;

        String[] order = {
                "module_123_song",
                "module_abc_song",
                "module_feed_the_monster",
                "module_match_the_letter",
                "module_memory_match",
                "module_word_builder",
                "module_my_family"
        };

        for (String id : order) {
            entries.add(new BarEntry(index, moduleValues.get(id)));
            labels.add(formatModuleLabel(id));
            index++;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Module Plays / Scores");
        int[] colors = new int[entries.size()];
        for (int i = 0; i < entries.size(); i++) {
            String id = order[i];
            colors[i] = "video".equals(moduleTypes.get(id))
                    ? Color.parseColor("#2196F3")
                    : Color.parseColor("#4CAF50");
        }
        dataSet.setColors(colors);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(11f);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.7f);
        chart.setData(data);

        XAxis xAxis = chart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setLabelRotationAngle(-45f);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);

        YAxis left = chart.getAxisLeft();
        left.setAxisMinimum(0f);
        left.setGranularity(1f);
        left.setTextSize(10f);

        chart.getAxisRight().setEnabled(false);
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(true);
        chart.setExtraOffsets(10f, 10f, 10f, 30f);
        chart.setFitBars(true);
        chart.animateY(1000);
        chart.invalidate();

        Log.d(TAG, "✅ Chart loaded successfully with " + entries.size() + " modules");
    }

    // Format module names for chart labels
    private String formatModuleLabel(String id) {
        switch (id) {
            case "module_123_song": return "123 Song";
            case "module_abc_song": return "ABC Song";
            case "module_feed_the_monster": return "Feed Monster";
            case "module_match_the_letter": return "Match Letter";
            case "module_memory_match": return "Memory Match";
            case "module_word_builder": return "Word Builder";
            case "module_my_family": return "My Family";
            default: return id;
        }
    }

    // Create a card for each child (with progress % and stars)
    private CardView createChildCard(ChildProfile child, List<Progress> progressList) {
        CardView card = (CardView) getLayoutInflater().inflate(R.layout.item_child_card_attractive, childrenContainer, false);

        ImageView avatar = card.findViewById(R.id.imgChildAvatar);
        TextView name = card.findViewById(R.id.txtChildName);
        TextView age = card.findViewById(R.id.txtChildAge);
        TextView progressText = card.findViewById(R.id.txtProgress);
        ProgressBar progressBar = card.findViewById(R.id.progressBar);
        LinearLayout achievementsLayout = card.findViewById(R.id.layoutAchievements);

        // Use displayName with fallback to name
        String displayName = child.getDisplayName() != null ? child.getDisplayName() : child.getName();
        name.setText(displayName);
        age.setText(child.getAge() + " years old");

        // Load avatar (fallback placeholder)
        if (child.getAvatarUrl() != null && !child.getAvatarUrl().isEmpty()) {
            Glide.with(this)
                    .load(child.getAvatarUrl())
                    .placeholder(R.drawable.ic_child_avatar_placeholder)
                    .error(R.drawable.ic_child_avatar_placeholder)
                    .transform(new CircleCrop())
                    .into(avatar);
        } else {
            avatar.setImageResource(R.drawable.ic_child_avatar_placeholder);
        }

        // Calculate completion percentage based on 7 total modules
        int totalModules = 7;
        int completedModules = 0;
        int starsEarned = 0;

        if (progressList != null && !progressList.isEmpty()) {
            for (Progress p : progressList) {
                if (p == null || p.getModuleId() == null || !p.getChildId().equals(child.getChildId())) {
                    continue;
                }

                // If video was played or game scored above 0, count as completed
                if ("video".equalsIgnoreCase(p.getType()) && p.getPlays() > 0) {
                    completedModules++;
                } else if (p.getScore() > 0) {
                    completedModules++;
                }

                // Award star if score ≥ 80
                if (p.getScore() >= 80) {
                    starsEarned++;
                }
            }
        }

        // Compute % progress
        int percentage = (int) Math.round((completedModules / (double) totalModules) * 100);

        // Update progress UI
        if (completedModules == 0) {
            progressText.setText("New to BrightBuds!");
            progressText.setTextColor(Color.parseColor("#666666"));
            progressBar.setVisibility(View.GONE);
        } else {
            progressText.setText("Progress: " + percentage + "%");
            progressBar.setProgress(percentage);
            progressBar.setVisibility(View.VISIBLE);

            // Color-code progress text
            if (percentage >= 80) {
                progressText.setTextColor(Color.parseColor("#4CAF50")); // green
            } else if (percentage >= 50) {
                progressText.setTextColor(Color.parseColor("#FFC107")); // orange
            } else {
                progressText.setTextColor(Color.parseColor("#F44336")); // red
            }
        }

        // Add star achievements
        achievementsLayout.removeAllViews();
        if (starsEarned > 0) {
            for (int i = 0; i < starsEarned; i++) {
                ImageView star = new ImageView(this);
                star.setImageResource(R.drawable.ic_star_yellow);
                int size = (int) getResources().getDimension(R.dimen.star_icon_size);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
                params.setMargins(4, 0, 4, 0);
                star.setLayoutParams(params);
                achievementsLayout.addView(star);
            }
        } else {
            TextView noStar = new TextView(this);
            noStar.setText("⭐ 0 Stars");
            noStar.setTextColor(Color.GRAY);
            noStar.setTextSize(10);
            achievementsLayout.addView(noStar);
        }

        // On-click select feedback
        card.setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences("BrightBudsPrefs", MODE_PRIVATE);
            prefs.edit()
                    .putString("selectedChildId", child.getChildId())
                    .putString("selectedChildName", displayName) // Use the correct display name
                    .apply();

            card.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100)
                    .withEndAction(() -> card.animate().scaleX(1f).scaleY(1f).setDuration(100));

            Toast.makeText(this, "Selected " + displayName + " ✨", Toast.LENGTH_SHORT).show();
        });

        return card;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "🔄 Refreshing dashboard data");
        loadChildrenAndProgress(); // Refresh dashboard
    }
}