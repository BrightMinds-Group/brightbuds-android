package com.example.brightbuds_app.ui.games;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.brightbuds_app.R;
import com.example.brightbuds_app.interfaces.DataCallbacks;
import com.example.brightbuds_app.services.ProgressService;
import com.example.brightbuds_app.utils.Constants;

import java.util.HashSet;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

/**
 * FeedTheMonsterFragment
 *
 * Educational mini-game where the child matches the target number of cookies.
 * Tracks performance metrics and stores results locally and on Firebase.
 */
public class FeedTheMonsterFragment extends Fragment {

    // UI elements
    private ImageView bgImage, imgMonster, imgStar;
    private TextView tvScore, tvRound, tvTarget, tvStats;
    private GridLayout gridCookies;
    private Button btnReset, btnNext;
    private ImageButton btnHomeIcon, btnCloseIcon;
    private ProgressBar progressRound;

    // Game state
    private final Random rng = new Random();
    private final Set<View> selectedCookies = new HashSet<>();
    private int score = 0;
    private int round = 1;
    private int targetNumber = 5;
    private int totalCorrect = 0;
    private int totalIncorrect = 0;
    private int wrongStreak = 0;
    private int stars = 0;

    // Session tracking
    private long sessionStartMs = 0L;
    private int sessionRounds = 0;
    private int timesPlayed;

    // Shared Preferences keys
    private static final String PREFS = "brightbuds_game_prefs";
    private static final String KEY_TIMES_PLAYED = "feed_monster_times_played";

    // Services and Audio
    private ProgressService progressService;
    private MediaPlayer bgMusic;
    private TextToSpeech tts;

    // Selected child reference
    private String selectedChildId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_feed_the_monster, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        // Bind UI components
        bgImage = v.findViewById(R.id.bgImage);
        imgMonster = v.findViewById(R.id.imgMonster);
        imgStar = v.findViewById(R.id.imgStar);
        tvScore = v.findViewById(R.id.tvScore);
        tvRound = v.findViewById(R.id.tvRound);
        tvTarget = v.findViewById(R.id.tvTarget);
        tvStats = v.findViewById(R.id.tvStats);
        gridCookies = v.findViewById(R.id.gridCookies);
        btnReset = v.findViewById(R.id.btnReset);
        btnNext = v.findViewById(R.id.btnNext);
        btnHomeIcon = v.findViewById(R.id.btnHomeIcon);
        btnCloseIcon = v.findViewById(R.id.btnCloseIcon);
        progressRound = v.findViewById(R.id.progressRound);

        // Initialize ProgressService and get child reference
        progressService = new ProgressService(requireContext());
        SharedPreferences parentPrefs = requireContext().getSharedPreferences("BrightBudsPrefs", Context.MODE_PRIVATE);
        selectedChildId = parentPrefs.getString("selectedChildId", null);

        // Create cookies dynamically
        createCookieButtons(10);

        // Button actions
        btnReset.setOnClickListener(view -> resetSelection());
        btnNext.setOnClickListener(view -> checkAndAdvance());

        // Close and Home navigation
        View.OnClickListener endGame = vv -> {
            saveSessionMetricsSafely();
            stopAudioTts();
            requireActivity().finish();
        };
        btnHomeIcon.setOnClickListener(endGame);
        btnCloseIcon.setOnClickListener(endGame);

        // Background music setup
        bgMusic = MediaPlayer.create(requireContext(), R.raw.monster_music);
        if (bgMusic != null) {
            bgMusic.setLooping(true);
            bgMusic.setVolume(0.25f, 0.25f);
            bgMusic.start();
        }

        // Text-to-Speech setup
        tts = new TextToSpeech(requireContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.ENGLISH);
                tts.setPitch(1.1f);
                tts.setSpeechRate(0.95f);
            }
        });

        // Persistent play tracking
        SharedPreferences sp = requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        timesPlayed = sp.getInt(KEY_TIMES_PLAYED, 0) + 1;
        sp.edit().putInt(KEY_TIMES_PLAYED, timesPlayed).apply();

        // Start gameplay
        sessionStartMs = System.currentTimeMillis();
        startRound(true);
        startBackgroundPan();
    }

    /** Creates cookie buttons dynamically for gameplay. */
    private void createCookieButtons(int totalCookies) {
        int sizePx = dp(64), padPx = dp(8);
        for (int i = 0; i < totalCookies; i++) {
            ImageButton cookie = new ImageButton(requireContext());
            cookie.setBackground(null);
            cookie.setScaleType(ImageView.ScaleType.FIT_CENTER);
            cookie.setPadding(padPx, padPx, padPx, padPx);
            cookie.setImageResource(R.drawable.cookie);
            cookie.setFocusable(true);
            cookie.setContentDescription("Cookie");

            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = sizePx;
            lp.height = sizePx;
            lp.setMargins(padPx, padPx, padPx, padPx);
            cookie.setLayoutParams(lp);

            cookie.setOnClickListener(this::onCookieTapped);
            gridCookies.addView(cookie);
        }
    }

    /** Handles cookie selection logic. */
    private void onCookieTapped(View cookie) {
        if (selectedCookies.contains(cookie)) {
            selectedCookies.remove(cookie);
            animateScale(cookie, 1.0f);
            cookie.setAlpha(1.0f);
        } else {
            selectedCookies.add(cookie);
            animateScale(cookie, 1.15f);
            cookie.setAlpha(0.85f);
            speak(String.valueOf(selectedCookies.size()));
        }
        progressRound.setMax(targetNumber);
        progressRound.setProgress(Math.min(selectedCookies.size(), targetNumber));
    }

    /** Starts a new round and resets game state. */
    private void startRound(boolean first) {
        targetNumber = 1 + rng.nextInt(10);
        selectedCookies.clear();
        wrongStreak = 0;

        for (int i = 0; i < gridCookies.getChildCount(); i++) {
            View child = gridCookies.getChildAt(i);
            child.setAlpha(1f);
            animateScale(child, 1.0f);
        }

        imgMonster.setImageResource(R.drawable.monster_neutral);
        tvTarget.setText("Feed me: " + targetNumber);
        tvRound.setText("Round: " + round);
        tvScore.setText("Score: " + score);
        updateStats();

        progressRound.setMax(targetNumber);
        progressRound.setProgress(0);
        speak("Feed me " + targetNumber + " cookies");
        pulse(tvTarget);
    }

    /** Checks user answer and updates metrics. */
    private void checkAndAdvance() {
        int selected = selectedCookies.size();
        boolean correct = selected == targetNumber;

        if (correct) {
            totalCorrect++;
            wrongStreak = 0;
            score += 10;
            stars++;
            imgMonster.setImageResource(R.drawable.monster_happy);
            showStarFlash();
            wiggle(imgMonster);
            speak("Yay!");
        } else {
            totalIncorrect++;
            wrongStreak++;
            imgMonster.setImageResource(R.drawable.monster_sad);
            shake(imgMonster);
            speak("Try again!");
        }

        saveSessionMetricsIncremental();
        round++;
        sessionRounds++;
        startRound(false);
    }

    /** Displays a brief star flash animation for positive feedback. */
    private void showStarFlash() {
        imgStar.setVisibility(View.VISIBLE);
        animateScale(imgStar, 1.4f);
        imgStar.postDelayed(() -> {
            animateScale(imgStar, 1.0f);
            imgStar.setVisibility(View.GONE);
        }, 600);
    }

    /** Updates player performance statistics on screen. */
    private void updateStats() {
        tvStats.setText("Correct: " + totalCorrect
                + "  Incorrect: " + totalIncorrect
                + "  Played: " + timesPlayed
                + "  Stars: " + stars);
    }

    /** Animates background panning for dynamic feel. */
    private void startBackgroundPan() {
        bgImage.post(() -> {
            float shift = dp(32);
            ObjectAnimator left = ObjectAnimator.ofFloat(bgImage, View.TRANSLATION_X, -shift);
            left.setDuration(8000);
            left.setRepeatMode(ObjectAnimator.REVERSE);
            left.setRepeatCount(ObjectAnimator.INFINITE);
            left.start();
        });
    }

    /** Handles voice output for game prompts. */
    private void speak(String text) {
        if (tts == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "feed_monster_tts");
        } else {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    // ---------- Data persistence ----------

    private void saveSessionMetricsIncremental() {
        if (selectedChildId == null) return;
        long now = System.currentTimeMillis();
        long timeSpent = Math.max(0L, now - sessionStartMs);
        int plays = Math.max(1, timesPlayed);

        progressService.recordGameSession(
                selectedChildId,
                Constants.GAME_FEED_MONSTER,
                score,
                timeSpent,
                stars,
                totalCorrect,
                totalIncorrect,
                plays,
                new DataCallbacks.GenericCallback() {
                    @Override public void onSuccess(String result) { }
                    @Override public void onFailure(Exception e) { }
                }
        );
        updateStats();
    }

    private void saveSessionMetricsSafely() {
        if (selectedChildId == null) return;
        long now = System.currentTimeMillis();
        long timeSpent = Math.max(0L, now - sessionStartMs);
        int plays = Math.max(1, timesPlayed);

        progressService.recordGameSession(
                selectedChildId,
                Constants.GAME_FEED_MONSTER,
                score,
                timeSpent,
                stars,
                totalCorrect,
                totalIncorrect,
                plays,
                new DataCallbacks.GenericCallback() {
                    @Override public void onSuccess(String result) { }
                    @Override public void onFailure(Exception e) { }
                }
        );
    }

    // ---------- Animations ----------

    private void animateScale(View v, float toScale) {
        ObjectAnimator sx = ObjectAnimator.ofFloat(v, View.SCALE_X, toScale);
        ObjectAnimator sy = ObjectAnimator.ofFloat(v, View.SCALE_Y, toScale);
        sx.setDuration(120);
        sy.setDuration(120);
        sx.start();
        sy.start();
    }

    private void pulse(View v) {
        ObjectAnimator upX = ObjectAnimator.ofFloat(v, View.SCALE_X, 1f, 1.1f);
        ObjectAnimator upY = ObjectAnimator.ofFloat(v, View.SCALE_Y, 1f, 1.1f);
        upX.setDuration(160);
        upY.setDuration(160);
        upX.start();
        upY.start();
        v.postDelayed(() -> {
            ObjectAnimator downX = ObjectAnimator.ofFloat(v, View.SCALE_X, 1.1f, 1f);
            ObjectAnimator downY = ObjectAnimator.ofFloat(v, View.SCALE_Y, 1.1f, 1f);
            downX.setDuration(160);
            downY.setDuration(160);
            downX.start();
            downY.start();
        }, 180);
    }

    private void wiggle(View v) {
        ObjectAnimator r1 = ObjectAnimator.ofFloat(v, View.ROTATION, -8f);
        ObjectAnimator r2 = ObjectAnimator.ofFloat(v, View.ROTATION, 8f);
        ObjectAnimator r3 = ObjectAnimator.ofFloat(v, View.ROTATION, 0f);
        r1.setDuration(80);
        r2.setDuration(80);
        r3.setDuration(80);
        r1.start();
        v.postDelayed(r2::start, 90);
        v.postDelayed(r3::start, 180);
    }

    private void shake(View v) {
        ObjectAnimator r1 = ObjectAnimator.ofFloat(v, View.TRANSLATION_X, -dp(8));
        ObjectAnimator r2 = ObjectAnimator.ofFloat(v, View.TRANSLATION_X, dp(8));
        ObjectAnimator r3 = ObjectAnimator.ofFloat(v, View.TRANSLATION_X, 0);
        r1.setDuration(70);
        r2.setDuration(70);
        r3.setDuration(70);
        r1.start();
        v.postDelayed(r2::start, 80);
        v.postDelayed(r3::start, 160);
    }

    // ---------- Lifecycle ----------

    @Override
    public void onPause() {
        super.onPause();
        if (bgMusic != null && bgMusic.isPlaying()) bgMusic.pause();
        saveSessionMetricsSafely();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (bgMusic != null) bgMusic.start();
    }

    @Override
    public void onDestroyView() {
        saveSessionMetricsSafely();
        super.onDestroyView();
        stopAudioTts();
    }

    // ---------- Utilities ----------

    private int dp(int value) {
        float scale = getResources().getDisplayMetrics().density;
        return (int) (value * scale + 0.5f);
    }

    private void stopAudioTts() {
        if (bgMusic != null) {
            try { if (bgMusic.isPlaying()) bgMusic.stop(); } catch (Exception ignored) { }
            bgMusic.release();
            bgMusic = null;
        }
        if (tts != null) {
            try { tts.stop(); } catch (Exception ignored) { }
            tts.shutdown();
            tts = null;
        }
    }

    /** Clears selected cookies and resets round progress. */
    private void resetSelection() {
        selectedCookies.clear();
        for (int i = 0; i < gridCookies.getChildCount(); i++) {
            View child = gridCookies.getChildAt(i);
            child.setAlpha(1f);
            animateScale(child, 1.0f);
        }
        progressRound.setProgress(0);
        speak("Reset");
    }
}
