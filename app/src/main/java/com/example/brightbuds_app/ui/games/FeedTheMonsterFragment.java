package com.example.brightbuds_app.ui.games;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.brightbuds_app.R;
import com.example.brightbuds_app.interfaces.DataCallbacks;
import com.example.brightbuds_app.services.ProgressService;
import com.example.brightbuds_app.utils.Constants;

import java.util.Locale;
import java.util.Random;

/**
 * FeedTheMonsterFragment
 *
 * Educational mini game where the child drags cookies into the monster mouth
 * to match the displayed target number.
 */
public class FeedTheMonsterFragment extends Fragment {

    // UI elements
    private ImageView bgImage;
    private ImageView imgMonster;
    private ImageView imgStar;
    private ImageView imgTargetNumber;
    private TextView tvScore;
    private TextView tvRound;
    private TextView tvTarget;
    private TextView tvStats;
    private ProgressBar progressRound;
    private FrameLayout playArea;
    private ImageButton btnHomeIcon;
    private ImageButton btnCloseIcon;

    // Game state
    private final Random rng = new Random();
    private int score = 0;
    private int round = 1;
    private int targetNumber = 5;
    private int totalCorrect = 0;
    private int totalIncorrect = 0;
    private int wrongStreak = 0;
    private int stars = 0;
    private int cookiesFedThisRound = 0;
    private boolean roundLocked = false;

    // Session tracking
    private long sessionStartMs = 0L;
    private int sessionRounds = 0;
    private int timesPlayed;

    // Shared preferences keys
    private static final String PREFS = "brightbuds_game_prefs";
    private static final String KEY_TIMES_PLAYED = "feed_monster_times_played";

    // Services and audio
    private ProgressService progressService;
    private MediaPlayer bgMusic;
    private TextToSpeech tts;

    // Child selection
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
        imgTargetNumber = v.findViewById(R.id.imgTargetNumber);
        tvScore = v.findViewById(R.id.tvScore);
        tvRound = v.findViewById(R.id.tvRound);
        tvTarget = v.findViewById(R.id.tvTarget);
        tvStats = v.findViewById(R.id.tvStats);
        progressRound = v.findViewById(R.id.progressRound);
        playArea = v.findViewById(R.id.playArea);
        btnHomeIcon = v.findViewById(R.id.btnHomeIcon);
        btnCloseIcon = v.findViewById(R.id.btnCloseIcon);

        // Initialise services and child reference
        progressService = new ProgressService(requireContext());
        SharedPreferences parentPrefs =
                requireContext().getSharedPreferences("BrightBudsPrefs", Context.MODE_PRIVATE);
        selectedChildId = parentPrefs.getString("selectedChildId", null);

        // Background music setup
        bgMusic = MediaPlayer.create(requireContext(), R.raw.monster_music);
        if (bgMusic != null) {
            bgMusic.setLooping(true);
            bgMusic.setVolume(0.25f, 0.25f);
            bgMusic.start();
        }

        // Text to speech setup
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

        // Home and Close both just finish this screen
        View.OnClickListener endGame = view1 -> {
            saveSessionMetricsSafely();
            stopAudioTts();
            requireActivity().finish();
        };
        btnHomeIcon.setOnClickListener(endGame);
        btnCloseIcon.setOnClickListener(endGame);

        // Start session and first round
        sessionStartMs = System.currentTimeMillis();
        startRound(true);
    }

    // region Game setup and rounds

    private void startRound(boolean firstRound) {
        roundLocked = false;
        cookiesFedThisRound = 0;
        wrongStreak = 0;

        // Target number between 1 and 10
        targetNumber = 1 + rng.nextInt(10);

        imgMonster.setImageResource(R.drawable.monster_neutral);
        tvTarget.setText("Feed me");
        tvRound.setText("Round: " + round);
        tvScore.setText("Score: " + score);
        updateStats();

        progressRound.setMax(targetNumber);
        progressRound.setProgress(0);

        updateTargetNumberImage(targetNumber);

        playArea.removeAllViews();
        // ten cookies each round
        createCookiesForRound(10);

        speak("Feed me " + targetNumber + " cookies");
        pulse(tvTarget);
    }

    private void updateTargetNumberImage(int number) {
        int resId;
        switch (number) {
            case 1:  resId = R.drawable.number_1;  break;
            case 2:  resId = R.drawable.number_2;  break;
            case 3:  resId = R.drawable.number_3;  break;
            case 4:  resId = R.drawable.number_4;  break;
            case 5:  resId = R.drawable.number_5;  break;
            case 6:  resId = R.drawable.number_6;  break;
            case 7:  resId = R.drawable.number_7;  break;
            case 8:  resId = R.drawable.number_8;  break;
            case 9:  resId = R.drawable.number_9;  break;
            case 10: resId = R.drawable.number_10; break;
            default: resId = 0;                     break;
        }

        if (resId != 0) {
            imgTargetNumber.setVisibility(View.VISIBLE);
            imgTargetNumber.setImageResource(resId);
        } else {
            imgTargetNumber.setVisibility(View.INVISIBLE);
        }
    }

    private void createCookiesForRound(int cookieCount) {
        playArea.post(() -> {
            int width = playArea.getWidth();
            int height = playArea.getHeight();

            if (width <= 0 || height <= 0) return;

            int size = dp(110); // cookie size
            int margin = dp(6);

            for (int i = 0; i < cookieCount; i++) {
                ImageView cookie = new ImageView(requireContext());
                cookie.setImageResource(R.drawable.cookie);
                cookie.setContentDescription("Cookie");
                cookie.setScaleType(ImageView.ScaleType.FIT_CENTER);

                FrameLayout.LayoutParams lp =
                        new FrameLayout.LayoutParams(size, size);
                cookie.setLayoutParams(lp);

                // Let cookies START on the right half of the screen
                int totalMaxX = width - size - margin;
                int halfWidth = width / 2;
                int minX = halfWidth;
                int maxX = totalMaxX;

                float startX = minX + rng.nextFloat() * Math.max(1, (maxX - minX));
                float startY = margin + rng.nextFloat() * Math.max(1, (height - size - margin));

                cookie.setX(startX);
                cookie.setY(startY);

                cookie.setOnTouchListener(cookieDragListener);
                playArea.addView(cookie);
            }
        });
    }



    // endregion

    // region Drag and drop logic

    private final View.OnTouchListener cookieDragListener = new View.OnTouchListener() {

        float dX;
        float dY;
        final int[] playAreaLocation = new int[2];

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            if (roundLocked) {
                return false;
            }

            playArea.getLocationOnScreen(playAreaLocation);

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: {
                    float touchX = event.getRawX() - playAreaLocation[0];
                    float touchY = event.getRawY() - playAreaLocation[1];
                    dX = view.getX() - touchX;
                    dY = view.getY() - touchY;
                    return true;
                }
                case MotionEvent.ACTION_MOVE: {
                    float newX = event.getRawX() - playAreaLocation[0] + dX;
                    float newY = event.getRawY() - playAreaLocation[1] + dY;

                    float maxX = playArea.getWidth() - view.getWidth();
                    float maxY = playArea.getHeight() - view.getHeight();
                    newX = Math.max(0, Math.min(newX, maxX));
                    newY = Math.max(0, Math.min(newY, maxY));

                    view.setX(newX);
                    view.setY(newY);
                    return true;
                }
                case MotionEvent.ACTION_UP: {
                    handleCookieDrop(view);
                    return true;
                }
                default:
                    return false;
            }
        }
    };

    private void handleCookieDrop(View cookieView) {
        if (roundLocked) {
            return;
        }

        Rect monsterRect = new Rect();
        Rect cookieRect = new Rect();

        imgMonster.getGlobalVisibleRect(monsterRect);
        cookieView.getGlobalVisibleRect(cookieRect);

        boolean hitMonster = Rect.intersects(monsterRect, cookieRect);

        if (hitMonster) {
            handleCookieFed(cookieView);
        } else {
            handleMiss();
        }
    }

    private void handleCookieFed(View cookieView) {
        cookieView.setVisibility(View.INVISIBLE);
        cookiesFedThisRound++;

        progressRound.setProgress(Math.min(cookiesFedThisRound, targetNumber));
        speak(String.valueOf(cookiesFedThisRound));

        if (cookiesFedThisRound == targetNumber) {
            handleCorrectAnswer();
        }
    }

    private void handleMiss() {
        totalIncorrect++;
        wrongStreak++;
        imgMonster.setImageResource(R.drawable.monster_sad);
        shake(imgMonster);
        speak("Try again");
        saveSessionMetricsIncremental();
        updateStats();

        if (wrongStreak >= 5 && !roundLocked) {
            roundLocked = true;
            imgMonster.postDelayed(this::advanceRound, 800);
        }
    }

    private void handleCorrectAnswer() {
        roundLocked = true;
        totalCorrect++;
        wrongStreak = 0;
        score += 10;
        stars++;

        imgMonster.setImageResource(R.drawable.monster_happy);
        showStarFlash();
        wiggle(imgMonster);
        speak("Yay");
        saveSessionMetricsIncremental();
        updateStats();

        imgMonster.postDelayed(this::advanceRound, 900);
    }

    private void advanceRound() {
        round++;
        sessionRounds++;
        startRound(false);
    }

    // endregion

    // region Visual feedback and animations

    private void updateStats() {
        tvStats.setText(
                "Correct: " + totalCorrect
                        + "  Incorrect: " + totalIncorrect
                        + "  Played: " + timesPlayed
                        + "  Stars: " + stars
        );
    }

    private void showStarFlash() {
        imgStar.setVisibility(View.VISIBLE);
        animateScale(imgStar, 1.4f);
        imgStar.postDelayed(() -> {
            animateScale(imgStar, 1.0f);
            imgStar.setVisibility(View.GONE);
        }, 600);
    }

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

    // endregion

    // region Persistence

    private void saveSessionMetricsIncremental() {
        if (selectedChildId == null) {
            return;
        }

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
                    @Override
                    public void onSuccess(String result) { }

                    @Override
                    public void onFailure(Exception e) { }
                }
        );
    }

    private void saveSessionMetricsSafely() {
        if (selectedChildId == null) {
            return;
        }

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
                    @Override
                    public void onSuccess(String result) { }

                    @Override
                    public void onFailure(Exception e) { }
                }
        );
    }

    // endregion

    // region Utilities and lifecycle

    private void speak(String text) {
        if (tts == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "feed_monster_tts");
        } else {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    private int dp(int value) {
        float scale = getResources().getDisplayMetrics().density;
        return (int) (value * scale + 0.5f);
    }

    private void stopAudioTts() {
        if (bgMusic != null) {
            try {
                if (bgMusic.isPlaying()) {
                    bgMusic.stop();
                }
            } catch (Exception ignored) { }
            bgMusic.release();
            bgMusic = null;
        }
        if (tts != null) {
            try {
                tts.stop();
            } catch (Exception ignored) { }
            tts.shutdown();
            tts = null;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (bgMusic != null && bgMusic.isPlaying()) {
            bgMusic.pause();
        }
        saveSessionMetricsSafely();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (bgMusic != null) {
            bgMusic.start();
        }
    }

    @Override
    public void onDestroyView() {
        saveSessionMetricsSafely();
        super.onDestroyView();
        stopAudioTts();
    }

    // endregion
}
