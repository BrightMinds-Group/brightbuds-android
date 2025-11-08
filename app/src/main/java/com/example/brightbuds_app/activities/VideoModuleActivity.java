package com.example.brightbuds_app.activities;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;

import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;

import com.example.brightbuds_app.R;
import com.example.brightbuds_app.services.ProgressService;
import com.example.brightbuds_app.services.StorageService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class VideoModuleActivity extends AppCompatActivity {

    private static final String TAG = "VideoModuleActivity";

    private PlayerView playerView;
    private ExoPlayer player;
    private ProgressBar progressBar;
    private TextView txtStatus, txtChildName;
    private MaterialButton btnPlayPause, btnStop;
    private String storagePath, moduleId, moduleTitle, childId, moduleType; // ✅ added moduleType
    private ProgressService progressService;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_module);

        // Initialize services
        progressService = new ProgressService(this);

        // Bind views
        playerView = findViewById(R.id.playerView);
        progressBar = findViewById(R.id.progress);
        txtStatus = findViewById(R.id.txtStatus);
        txtChildName = findViewById(R.id.txtChildName);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnStop = findViewById(R.id.btnStop);

        // Get intent extras
        storagePath = getIntent().getStringExtra("storagePath");
        moduleId = getIntent().getStringExtra("moduleId");
        moduleTitle = getIntent().getStringExtra("moduleTitle");
        childId = getIntent().getStringExtra("childId");
        moduleType = getIntent().getStringExtra("moduleType"); // ✅ ensures Firestore knows it’s a video

        txtChildName.setText(moduleTitle != null ? moduleTitle : "Learning Video");

        if (storagePath == null || storagePath.isEmpty()) {
            Toast.makeText(this, "⚠️ Missing video path for this module.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        Log.d(TAG, "🎬 Loading video from Firebase path: " + storagePath);
        loadVideoFromFirebase(storagePath);

        // Button actions
        setupControls();
    }

    /** Setup play/pause/stop controls */
    private void setupControls() {
        btnPlayPause.setOnClickListener(v -> {
            if (player == null) return;

            if (player.isPlaying()) {
                player.pause();
                btnPlayPause.setText("Resume");
                txtStatus.setText("⏸ Paused");
            } else {
                player.play();
                btnPlayPause.setText("Pause");
                txtStatus.setText("▶ Playing");
            }
        });

        btnStop.setOnClickListener(v -> {
            if (player == null) return;

            player.stop();
            player.seekTo(0);
            txtStatus.setText("⏹ Stopped");
            btnPlayPause.setText("Play");
        });
    }

    /** Load video from Firebase Storage */
    private void loadVideoFromFirebase(String path) {
        StorageService.getInstance().getOrDownloadFile(
                this,
                path,
                uri -> runOnUiThread(() -> setupPlayer(uri)),
                e -> {
                    Log.e(TAG, "❌ Video download failed: " + e.getMessage());
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        txtStatus.setText("❌ Failed to load video");
                        Toast.makeText(this, "Video load error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
        );
    }

    /** Setup video player */
    @OptIn(markerClass = UnstableApi.class)
    private void setupPlayer(Uri uri) {
        try {
            if (player != null) player.release();

            player = new ExoPlayer.Builder(this).build();
            playerView.setPlayer(player);
            playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);

            // Audio setup
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build();
            player.setAudioAttributes(audioAttributes, true);

            MediaItem mediaItem = MediaItem.fromUri(uri);
            player.setMediaItem(mediaItem);
            player.prepare();
            player.setPlayWhenReady(true);

            // Player listener: triggers on completion
            player.addListener(new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int state) {
                    if (state == Player.STATE_READY) {
                        progressBar.setVisibility(View.GONE);
                        txtStatus.setText("▶ Playing");
                        btnPlayPause.setText("Pause");
                    } else if (state == Player.STATE_BUFFERING) {
                        progressBar.setVisibility(View.VISIBLE);
                        txtStatus.setText("⏳ Loading...");
                    } else if (state == Player.STATE_ENDED) {
                        txtStatus.setText("🎉 Completed");
                        btnPlayPause.setText("Replay");
                        // Save progress when video ends
                        logVideoPlay();
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "❌ Player setup failed: " + e.getMessage());
            Toast.makeText(this, "Player error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /** Log video play count in Firestore */
    private void logVideoPlay() {
        if (childId == null || moduleId == null) return;

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        String parentId = user.getUid();

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("child_progress")
                .whereEqualTo("parentId", parentId)
                .whereEqualTo("childId", childId)
                .whereEqualTo("moduleId", moduleId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty()) {
                        // Update existing record
                        String docId = snapshot.getDocuments().get(0).getId();
                        Long currentPlays = snapshot.getDocuments().get(0).getLong("plays");
                        int newPlays = (currentPlays != null ? currentPlays.intValue() : 0) + 1;

                        db.collection("child_progress").document(docId)
                                .update("plays", newPlays,
                                        "type", "video",
                                        "status", "completed",
                                        "timestamp", System.currentTimeMillis())
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "🎬 Play updated: " + newPlays);
                                    saveProgress();
                                });
                    } else {
                        // Create new record
                        Map<String, Object> data = new HashMap<>();
                        data.put("parentId", parentId);
                        data.put("childId", childId);
                        data.put("moduleId", moduleId);
                        data.put("plays", 1);
                        data.put("type", "video");
                        data.put("status", "completed");
                        data.put("score", 100);
                        data.put("timestamp", System.currentTimeMillis());

                        db.collection("child_progress")
                                .add(data)
                                .addOnSuccessListener(docRef -> {
                                    Log.d(TAG, "🎬 New progress record created for " + moduleId);
                                    saveProgress();
                                });
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "⚠️ Failed to log video play", e));
    }

    /** Save module completion progress (kept same) */
    private void saveProgress() {
        if (childId == null || moduleId == null) return;
        String parentId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        progressService.setCompletionPercentage(parentId, childId, moduleId, 100,
                new com.example.brightbuds_app.interfaces.DataCallbacks.GenericCallback() {
                    @Override
                    public void onSuccess(String message) {
                        Toast.makeText(VideoModuleActivity.this, "✅ Progress saved!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(VideoModuleActivity.this, "⚠️ Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) player.pause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (player != null) {
            player.release();
            player = null;
        }
    }
}
