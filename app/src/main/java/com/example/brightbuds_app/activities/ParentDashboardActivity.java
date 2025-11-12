package com.example.brightbuds_app.activities;

import static com.example.brightbuds_app.R.*;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
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
import com.example.brightbuds_app.utils.EncryptionUtil;
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
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParentDashboardActivity extends AppCompatActivity {

    private static final String TAG = "ParentDashboard";

    private TextView txtWelcome;
    private LinearLayout childrenContainer;

    private String parentId;
    private AuthServices authService;
    private ChildProfileService childService;
    private ProgressService progressService;
    private FirebaseFirestore db;

    private boolean isLoadingChildren = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_dashboard);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        parentId = currentUser.getUid();

        authService = new AuthServices(this);
        childService = new ChildProfileService();
        progressService = new ProgressService(this);
        db = FirebaseFirestore.getInstance();

        txtWelcome = findViewById(R.id.txtWelcomeParent);
        childrenContainer = findViewById(R.id.childrenContainer);

        loadParentNameForWelcome();

        MaterialButton btnManageFamily = findViewById(R.id.btnManageFamily);
        btnManageFamily.setOnClickListener(v ->
                startActivity(new Intent(this, FamilyManagementActivity.class)));

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnItemSelectedListener(this::onBottomNavSelected);

        Log.d(TAG, "ParentDashboard loaded for parent: " + parentId);
    }

    private void loadParentNameForWelcome() {
        db.collection("users").document(parentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String encryptedName = documentSnapshot.getString("name");
                        String encryptedFullName = documentSnapshot.getString("fullName");

                        String decryptedName = EncryptionUtil.decrypt(encryptedName);
                        String decryptedFullName = EncryptionUtil.decrypt(encryptedFullName);

                        String displayName = !TextUtils.isEmpty(decryptedName) ? decryptedName :
                                !TextUtils.isEmpty(decryptedFullName) ? decryptedFullName :
                                        "Parent";

                        txtWelcome.setText("Welcome back " + displayName + "!");
                        Log.d(TAG, "Parent name decrypted: " + displayName);
                    } else {
                        String parentName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
                        txtWelcome.setText("Welcome back " + (parentName != null ? parentName : "Parent") + "!");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load parent name", e);
                    String parentName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
                    txtWelcome.setText("Welcome back " + (parentName != null ? parentName : "Parent") + "!");
                });
    }

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

    private void loadChildrenAndProgress() {
        if (isLoadingChildren) {
            Log.d(TAG, "Skipping duplicate loadChildrenAndProgress call");
            return;
        }
        isLoadingChildren = true;

        childrenContainer.removeAllViews();

        View loadingView = getLayoutInflater().inflate(R.layout.item_loading_children, childrenContainer, false);
        childrenContainer.addView(loadingView);

        Log.d(TAG, "Loading children and progress data");

        childService.getChildrenForCurrentParent(new DataCallbacks.ChildrenListCallback() {
            @Override
            public void onSuccess(List<ChildProfile> children) {
                childrenContainer.removeAllViews();

                Log.d(TAG, "Loaded " + children.size() + " children");

                if (children.isEmpty()) {
                    View emptyView = getLayoutInflater().inflate(R.layout.item_empty_children, childrenContainer, false);
                    childrenContainer.addView(emptyView);
                    loadModuleOverviewChart(new ArrayList<>());
                    isLoadingChildren = false;
                    return;
                }

                List<String> childIds = new ArrayList<>();
                for (ChildProfile child : children) childIds.add(child.getChildId());

                progressService.getAllProgressForParentWithChildren(parentId, childIds, new ProgressListCallback() {
                    @Override
                    public void onSuccess(List<Progress> progressList) {
                        Log.d(TAG, "Loaded " + progressList.size() + " progress records");

                        for (ChildProfile child : children) {
                            childrenContainer.addView(createChildCard(child, progressList));
                        }
                        loadModuleOverviewChart(progressList);
                        isLoadingChildren = false;
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "Failed to load progress", e);
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
                Log.e(TAG, "Failed to load children", e);

                View errorView = getLayoutInflater().inflate(R.layout.item_error_children, childrenContainer, false);
                childrenContainer.addView(errorView);
                loadModuleOverviewChart(new ArrayList<>());
                isLoadingChildren = false;
            }
        });
    }

    private void loadModuleOverviewChart(List<Progress> progressList) {
        BarChart chart = findViewById(R.id.moduleOverviewChart);
        if (chart == null) return;

        Map<String, String> moduleTypes = new HashMap<>();
        moduleTypes.put("module_123_song", "video");
        moduleTypes.put("module_abc_song", "video");
        moduleTypes.put("module_feed_the_monster", "game");
        moduleTypes.put("module_match_the_letter", "game");
        moduleTypes.put("module_memory_match", "game");
        moduleTypes.put("module_word_builder", "game");
        moduleTypes.put("module_my_family", "game");
        moduleTypes.put("game_shapes_match", "game"); // new module id

        Map<String, Integer> moduleValues = new HashMap<>();
        for (String module : moduleTypes.keySet()) moduleValues.put(module, 0);

        Log.d(TAG, "Progress list size: " + progressList.size());

        for (Progress progress : progressList) {
            String moduleId = progress.getModuleId();
            if (moduleId == null || !moduleTypes.containsKey(moduleId)) continue;

            String type = progress.getType() != null ? progress.getType() : moduleTypes.get(moduleId);

            if ("video".equals(type)) {
                int plays = progress.getPlays();
                moduleValues.put(moduleId, moduleValues.get(moduleId) + (plays > 0 ? plays : 1));
            } else {
                int score = (int) Math.round(progress.getScore());
                moduleValues.put(moduleId, Math.max(moduleValues.get(moduleId), score));
            }

            Log.d(TAG, "Module=" + moduleId + " | Type=" + type + " | Plays=" + progress.getPlays() + " | Score=" + progress.getScore());
        }

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
                "module_my_family",
                "game_shapes_match"   // new
        };

        for (String id : order) {
            entries.add(new BarEntry(index, moduleValues.get(id)));
            labels.add(formatModuleLabel(id));
            index++;
        }

        BarDataSet dataSet = new BarDataSet(entries, "");
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
        chart.getLegend().setEnabled(false);
        chart.setExtraOffsets(10f, 10f, 10f, 30f);
        chart.setFitBars(true);
        chart.animateY(1000);
        chart.invalidate();

        Log.d(TAG, "Chart loaded with " + entries.size() + " modules");
    }

    private String formatModuleLabel(String id) {
        switch (id) {
            case "module_123_song": return "123 Song";
            case "module_abc_song": return "ABC Song";
            case "module_feed_the_monster": return "Feed Monster";
            case "module_match_the_letter": return "Match Letter";
            case "module_memory_match": return "Memory Match";
            case "module_word_builder": return "Word Builder";
            case "module_my_family": return "My Family";
            case "game_shapes_match": return "Shapes Match";
            default: return id;
        }
    }

    private CardView createChildCard(ChildProfile child, List<Progress> progressList) {
        CardView card = (CardView) getLayoutInflater().inflate(R.layout.item_child_card_attractive, childrenContainer, false);

        ImageView avatar = card.findViewById(R.id.imgChildAvatar);
        TextView name = card.findViewById(R.id.txtChildName);
        TextView age = card.findViewById(R.id.txtChildAge);
        TextView progressText = card.findViewById(R.id.txtProgress);
        ProgressBar progressBar = card.findViewById(R.id.progressBar);
        LinearLayout achievementsLayout = card.findViewById(R.id.layoutAchievements);

        String displayName = child.getDisplayName() != null ? child.getDisplayName() : child.getName();
        name.setText(displayName);
        age.setText(child.getAge() + " years old");

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

        int totalModules = 7;
        int completedModules = 0;
        int starsEarned = 0;

        if (progressList != null && !progressList.isEmpty()) {
            for (Progress p : progressList) {
                if (p == null || p.getModuleId() == null || !p.getChildId().equals(child.getChildId())) continue;

                if ("video".equalsIgnoreCase(p.getType()) && p.getPlays() > 0) {
                    completedModules++;
                } else if (p.getScore() > 0) {
                    completedModules++;
                }

                if (p.getScore() >= 80) {
                    starsEarned++;
                }
            }
        }

        int percentage = (int) Math.round((completedModules / (double) totalModules) * 100);
        percentage = Math.min(percentage, 100);

        int starsEarnedCapped = Math.min(starsEarned, 5);

        if (completedModules == 0) {
            progressText.setText("New to BrightBuds!");
            progressText.setTextColor(Color.parseColor("#666666"));
            progressBar.setVisibility(View.GONE);
        } else {
            progressText.setText("Progress: " + percentage + "%");
            progressBar.setProgress(percentage);
            progressBar.setVisibility(View.VISIBLE);

            if (percentage >= 80) {
                progressText.setTextColor(Color.parseColor("#4CAF50"));
            } else if (percentage >= 50) {
                progressText.setTextColor(Color.parseColor("#FFC107"));
            } else {
                progressText.setTextColor(Color.parseColor("#F44336"));
            }
        }

        achievementsLayout.removeAllViews();
        if (starsEarnedCapped > 0) {
            for (int i = 0; i < starsEarnedCapped; i++) {
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

        card.setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences("BrightBudsPrefs", MODE_PRIVATE);
            prefs.edit()
                    .putString("selectedChildId", child.getChildId())
                    .putString("selectedChildName", displayName)
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
        Log.d(TAG, "Refreshing dashboard data");
        progressService.autoSyncOfflineProgress();
        loadChildrenAndProgress();
    }
}
