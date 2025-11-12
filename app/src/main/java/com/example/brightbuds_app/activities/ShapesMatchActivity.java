package com.example.brightbuds_app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.brightbuds_app.R;
import com.example.brightbuds_app.interfaces.DataCallbacks;
import com.example.brightbuds_app.services.DataSyncManager;
import com.example.brightbuds_app.services.ProgressService;
import com.example.brightbuds_app.utils.Constants;
import com.example.brightbuds_app.utils.FirebaseHelper;
import com.google.android.material.snackbar.Snackbar;

/*
 ShapesMatchActivity
 Minimal template for a new mini game.
 Records progress through ProgressService and attempts sync via DataSyncManager.
 Keeps logic isolated from core services and data flow.
*/
public class ShapesMatchActivity extends AppCompatActivity {

    public static final String EXTRA_CHILD_ID = "childId";
    private static final String TAG = "ShapesMatchActivity";

    private String childId;
    private long sessionStartMs;
    private int score;
    private int roundsPlayed;

    private ProgressService progressService;
    private DataSyncManager dataSyncManager;

    private TextView tvTitle, tvScore, tvRounds;
    private ProgressBar pbScore;
    private Button btnCorrect, btnIncorrect, btnEndRound, btnFinish;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shapes_match);

        try { FirebaseHelper.getInstance(this).logScreenView("ShapesMatch", getClass().getSimpleName()); } catch (Exception ignore) {}

        Intent i = getIntent();
        childId = i != null ? i.getStringExtra(EXTRA_CHILD_ID) : null;
        if (childId == null || childId.trim().isEmpty()) {
            Snackbar.make(findViewById(android.R.id.content), "Missing child id", Snackbar.LENGTH_LONG).show();
            finish();
            return;
        }

        progressService = new ProgressService(this);
        dataSyncManager = new DataSyncManager(this);

        sessionStartMs = SystemClock.elapsedRealtime();
        score = 0;
        roundsPlayed = 0;

        bindViews();
        wireUi();
        tvTitle.setText(Constants.getModuleDisplayName(Constants.GAME_SHAPES_MATCH));
        renderScore();
    }

    private void bindViews() {
        tvTitle = findViewById(R.id.tvTitle);
        tvScore = findViewById(R.id.tvScore);
        tvRounds = findViewById(R.id.tvRounds);
        pbScore = findViewById(R.id.pbScore);
        btnCorrect = findViewById(R.id.btnCorrect);
        btnIncorrect = findViewById(R.id.btnIncorrect);
        btnEndRound = findViewById(R.id.btnEndRound);
        btnFinish = findViewById(R.id.btnFinish);
    }

    private void wireUi() {
        btnCorrect.setOnClickListener(v -> { score = Math.min(100, score + 20); renderScore(); });
        btnIncorrect.setOnClickListener(v -> { score = Math.max(0, score - 5); renderScore(); });
        btnEndRound.setOnClickListener(v -> { roundsPlayed++; Snackbar.make(v, "Round " + roundsPlayed + " ended", Snackbar.LENGTH_SHORT).show(); });
        btnFinish.setOnClickListener(this::finishSessionAndSave);
    }

    private void renderScore() {
        tvScore.setText("Score: " + score);
        pbScore.setProgress(score);
        tvRounds.setText("Rounds: " + roundsPlayed);
    }

    private void finishSessionAndSave(View v) {
        progressService.markModuleCompleted(
                childId,
                Constants.GAME_SHAPES_MATCH,
                score,
                new DataCallbacks.GenericCallback() {
                    @Override public void onSuccess(String message) {
                        Snackbar.make(v, "Saved: " + message, Snackbar.LENGTH_LONG).show();
                        dataSyncManager.syncAllPendingChanges(new DataCallbacks.GenericCallback() {
                            @Override public void onSuccess(String s) { Log.d(TAG, s); }
                            @Override public void onFailure(Exception e) { Log.w(TAG, "Sync deferred", e); }
                        });
                        finish();
                    }
                    @Override public void onFailure(Exception e) {
                        Snackbar.make(v, "Saved locally. Will sync later.", Snackbar.LENGTH_LONG).show();
                        finish();
                    }
                }
        );
    }
}
