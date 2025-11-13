package com.example.brightbuds_app.activities;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.brightbuds_app.R;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import nl.dionsegijn.konfetti.core.Party;
import nl.dionsegijn.konfetti.core.PartyFactory;
import nl.dionsegijn.konfetti.core.emitter.Emitter;
import nl.dionsegijn.konfetti.core.models.Shape;
import nl.dionsegijn.konfetti.xml.KonfettiView;

public class MatchLetterActivity extends AppCompatActivity {

    private TextView centerLetter;
    private TextView letter1, letter2, letter3, letter4, letter5;
    private int currentLevel = 1;
    private int incorrectAttempts = 0;
    private ArrayList<Integer> levelCompletionStatus = new ArrayList<>();
    private MediaPlayer wellDoneSound;
    private MediaPlayer wrongSound;
    private ImageView characterView;
    private KonfettiView konfettiView;
    private GridLayout starAccumulationContainer;
    private boolean isAnimating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_match_letter);

        centerLetter = findViewById(R.id.centerLetter);
        letter1 = findViewById(R.id.letter1);
        letter2 = findViewById(R.id.letter2);
        letter3 = findViewById(R.id.letter3);
        letter4 = findViewById(R.id.letter4);
        letter5 = findViewById(R.id.letter5);
        ImageView closeButton = findViewById(R.id.closeButton);
        ImageView homeButton = findViewById(R.id.homeButton);
        characterView = findViewById(R.id.characterView);
        konfettiView = findViewById(R.id.konfettiView);
        starAccumulationContainer = findViewById(R.id.star_accumulation_container);

        wellDoneSound = MediaPlayer.create(this, R.raw.well_done_sound);
        wrongSound = MediaPlayer.create(this, R.raw.wrong);

        for (int i = 0; i < 26; i++) {
            levelCompletionStatus.add(0);
        }

        letter1.setOnTouchListener(new MyTouchListener());
        letter2.setOnTouchListener(new MyTouchListener());
        letter3.setOnTouchListener(new MyTouchListener());
        letter4.setOnTouchListener(new MyTouchListener());
        letter5.setOnTouchListener(new MyTouchListener());

        centerLetter.setOnDragListener(new MyDragListener());

        View.OnClickListener dashboardNavigation = v -> {
            Intent intent = new Intent(MatchLetterActivity.this, MatchLetterActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        };

        closeButton.setOnClickListener(dashboardNavigation);
        homeButton.setOnClickListener(dashboardNavigation);

        setupLevel(currentLevel);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (wellDoneSound != null) {
            wellDoneSound.release();
            wellDoneSound = null;
        }
        if (wrongSound != null) {
            wrongSound.release();
            wrongSound = null;
        }
    }

    private void setupLevel(int level) {
        isAnimating = false;
        incorrectAttempts = 0;
        updateStarDisplay();
        Glide.with(this).load(R.drawable.character_default).into(characterView);
        currentLevel = level;
        switch (currentLevel) {
            case 1:
                centerLetter.setText("A");
                letter1.setText("A");
                letter1.setTag("A");
                letter2.setText("B");
                letter2.setTag("B");
                letter3.setText("D");
                letter3.setTag("D");
                letter4.setText("G");
                letter4.setTag("G");
                letter5.setText("H");
                letter5.setTag("H");
                break;
            case 2:
                centerLetter.setText("L");
                letter1.setText("C");
                letter1.setTag("C");
                letter2.setText("K");
                letter2.setTag("K");
                letter3.setText("M");
                letter3.setTag("M");
                letter4.setText("L");
                letter4.setTag("L");
                letter5.setText("D");
                letter5.setTag("D");
                break;
            case 3:
                centerLetter.setText("C");
                letter1.setText("A");
                letter1.setTag("A");
                letter2.setText("S");
                letter2.setTag("S");
                letter3.setText("H");
                letter3.setTag("H");
                letter4.setText("C");
                letter4.setTag("C");
                letter5.setText("I");
                letter5.setTag("I");
                break;
            case 4:
                centerLetter.setText("S");
                letter1.setText("D");
                letter1.setTag("D");
                letter2.setText("H");
                letter2.setTag("H");
                letter3.setText("G");
                letter3.setTag("G");
                letter4.setText("S");
                letter4.setTag("S");
                letter5.setText("N");
                letter5.setTag("N");
                break;
            case 5:
                centerLetter.setText("E");
                letter1.setText("K");
                letter1.setTag("K");
                letter2.setText("I");
                letter2.setTag("I");
                letter3.setText("O");
                letter3.setTag("O");
                letter4.setText("E");
                letter4.setTag("E");
                letter5.setText("T");
                letter5.setTag("T");
                break;
            case 6:
                centerLetter.setText("T");
                letter1.setText("Y");
                letter1.setTag("Y");
                letter2.setText("E");
                letter2.setTag("E");
                letter3.setText("R");
                letter3.setTag("R");
                letter4.setText("G");
                letter4.setTag("G");
                letter5.setText("T");
                letter5.setTag("T");
                break;
            case 7:
                centerLetter.setText("N");
                letter1.setText("C");
                letter1.setTag("C");
                letter2.setText("X");
                letter2.setTag("X");
                letter3.setText("B");
                letter3.setTag("B");
                letter4.setText("N");
                letter4.setTag("N");
                letter5.setText("Q");
                letter5.setTag("Q");
                break;
            case 8:
                centerLetter.setText("P");
                letter1.setText("Y");
                letter1.setTag("Y");
                letter2.setText("P");
                letter2.setTag("P");
                letter3.setText("B");
                letter3.setTag("B");
                letter4.setText("S");
                letter4.setTag("S");
                letter5.setText("U");
                letter5.setTag("U");
                break;
            case 9:
                centerLetter.setText("Q");
                letter1.setText("I");
                letter1.setTag("I");
                letter2.setText("D");
                letter2.setTag("D");
                letter3.setText("F");
                letter3.setTag("F");
                letter4.setText("Q");
                letter4.setTag("Q");
                letter5.setText("X");
                letter5.setTag("X");
                break;
            case 10:
                centerLetter.setText("K");
                letter1.setText("J");
                letter1.setTag("J");
                letter2.setText("W");
                letter2.setTag("W");
                letter3.setText("K");
                letter3.setTag("K");
                letter4.setText("D");
                letter4.setTag("D");
                letter5.setText("Q");
                letter5.setTag("Q");
                break;
            case 11:
                centerLetter.setText("H");
                letter1.setText("A");
                letter1.setTag("A");
                letter2.setText("W");
                letter2.setTag("W");
                letter3.setText("V");
                letter3.setTag("V");
                letter4.setText("J");
                letter4.setTag("J");
                letter5.setText("H");
                letter5.setTag("H");
                break;
            case 12:
                centerLetter.setText("V");
                letter1.setText("V");
                letter1.setTag("V");
                letter2.setText("X");
                letter2.setTag("X");
                letter3.setText("F");
                letter3.setTag("F");
                letter4.setText("X");
                letter4.setTag("X");
                letter5.setText("P");
                letter5.setTag("P");
                break;
            case 13:
                centerLetter.setText("B");
                letter1.setText("T");
                letter1.setTag("T");
                letter2.setText("Y");
                letter2.setTag("Y");
                letter3.setText("C");
                letter3.setTag("C");
                letter4.setText("B");
                letter4.setTag("B");
                letter5.setText("Q");
                letter5.setTag("Q");
                break;
            case 14:
                centerLetter.setText("D");
                letter1.setText("I");
                letter1.setTag("I");
                letter2.setText("D");
                letter2.setTag("D");
                letter3.setText("M");
                letter3.setTag("M");
                letter4.setText("X");
                letter4.setTag("X");
                letter5.setText("P");
                letter5.setTag("P");
                break;
            case 15:
                centerLetter.setText("Z");
                letter1.setText("A");
                letter1.setTag("A");
                letter2.setText("R");
                letter2.setTag("R");
                letter3.setText("Z");
                letter3.setTag("Z");
                letter4.setText("D");
                letter4.setTag("D");
                letter5.setText("S");
                letter5.setTag("S");
                break;
            case 16:
                centerLetter.setText("J");
                letter1.setText("J");
                letter1.setTag("J");
                letter2.setText("Y");
                letter2.setTag("Y");
                letter3.setText("Q");
                letter3.setTag("Q");
                letter4.setText("W");
                letter4.setTag("W");
                letter5.setText("R");
                letter5.setTag("R");
                break;
            case 17:
                centerLetter.setText("U");
                letter1.setText("H");
                letter1.setTag("H");
                letter2.setText("U");
                letter2.setTag("U");
                letter3.setText("S");
                letter3.setTag("S");
                letter4.setText("E");
                letter4.setTag("E");
                letter5.setText("I");
                letter5.setTag("I");
                break;
            case 18:
                centerLetter.setText("R");
                letter1.setText("J");
                letter1.setTag("J");
                letter2.setText("M");
                letter2.setTag("M");
                letter3.setText("T");
                letter3.setTag("T");
                letter4.setText("N");
                letter4.setTag("N");
                letter5.setText("R");
                letter5.setTag("R");
                break;
            case 19:
                centerLetter.setText("M");
                letter1.setText("K");
                letter1.setTag("K");
                letter2.setText("Z");
                letter2.setTag("Z");
                letter3.setText("M");
                letter3.setTag("M");
                letter4.setText("N");
                letter4.setTag("N");
                letter5.setText("W");
                letter5.setTag("W");
                break;
            case 20:
                centerLetter.setText("Y");
                letter1.setText("O");
                letter1.setTag("O");
                letter2.setText("S");
                letter2.setTag("S");
                letter3.setText("E");
                letter3.setTag("E");
                letter4.setText("Y");
                letter4.setTag("Y");
                letter5.setText("B");
                letter5.setTag("B");
                break;
            case 21:
                centerLetter.setText("G");
                letter1.setText("G");
                letter1.setTag("G");
                letter2.setText("A");
                letter2.setTag("A");
                letter3.setText("Q");
                letter3.setTag("Q");
                letter4.setText("Z");
                letter4.setTag("Z");
                letter5.setText("U");
                letter5.setTag("U");
                break;
            case 22:
                centerLetter.setText("I");
                letter1.setText("D");
                letter1.setTag("D");
                letter2.setText("F");
                letter2.setTag("F");
                letter3.setText("I");
                letter3.setTag("I");
                letter4.setText("C");
                letter4.setTag("C");
                letter5.setText("T");
                letter5.setTag("T");
                break;
            case 23:
                centerLetter.setText("F");
                letter1.setText("A");
                letter1.setTag("A");
                letter2.setText("S");
                letter2.setTag("S");
                letter3.setText("P");
                letter3.setTag("P");
                letter4.setText("T");
                letter4.setTag("T");
                letter5.setText("F");
                letter5.setTag("F");
                break;
            case 24:
                centerLetter.setText("O");
                letter1.setText("R");
                letter1.setTag("R");
                letter2.setText("O");
                letter2.setTag("O");
                letter3.setText("F");
                letter3.setTag("F");
                letter4.setText("K");
                letter4.setTag("K");
                letter5.setText("J");
                letter5.setTag("J");
                break;
            case 25:
                centerLetter.setText("X");
                letter1.setText("X");
                letter1.setTag("X");
                letter2.setText("G");
                letter2.setTag("G");
                letter3.setText("K");
                letter3.setTag("K");
                letter4.setText("D");
                letter4.setTag("D");
                letter5.setText("V");
                letter5.setTag("V");
                break;
            case 26:
                centerLetter.setText("W");
                letter1.setText("I");
                letter1.setTag("I");
                letter2.setText("W");
                letter2.setTag("W");
                letter3.setText("Q");
                letter3.setTag("Q");
                letter4.setText("H");
                letter4.setTag("H");
                letter5.setText("S");
                letter5.setTag("S");
                break;
            default:
                Intent intent = new Intent(MatchLetterActivity.this, CongratulationsActivity.class);
                long totalStars = levelCompletionStatus.stream().filter(s -> s == 1).count();
                intent.putExtra("totalStars", (int) totalStars);
                startActivity(intent);
                finish();
                break;
        }
    }

    private void nextLevel() {
        setupLevel(currentLevel + 1);
    }

    private void showCustomToast(String message, boolean isCorrect) {
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.custom_toast,
                (ViewGroup) findViewById(R.id.toast_layout_root));

        ImageView toastIcon = layout.findViewById(R.id.toast_icon);
        TextView toastText = layout.findViewById(R.id.toast_text);

        toastText.setText(message);

        if (isCorrect) {
            showConfetti();
            Glide.with(this).load(R.drawable.character_happy).into(characterView);
            layout.setBackgroundResource(R.drawable.toast_background_correct);
            toastIcon.setImageResource(R.drawable.ic_thumb_up);
            if (wellDoneSound != null) {
                if (wellDoneSound.isPlaying()) {
                    wellDoneSound.pause();
                }
                wellDoneSound.seekTo(0);
                wellDoneSound.start();
            }
        } else {
            incorrectAttempts++;
            Glide.with(this).load(R.drawable.character_sad).into(characterView);
            layout.setBackgroundResource(R.drawable.toast_background_incorrect);
            toastIcon.setImageResource(R.drawable.ic_close);
            if (wrongSound != null) {
                if (wrongSound.isPlaying()) {
                    wrongSound.pause();
                }
                wrongSound.seekTo(0);
                wrongSound.start();
            }
        }

        Toast toast = new Toast(getApplicationContext());
        toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }

    private void updateStarDisplay() {
        starAccumulationContainer.removeAllViews();
        for (Integer status : levelCompletionStatus) {
            ImageView star = new ImageView(this);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 96;
            params.height = 96;
            star.setLayoutParams(params);

            if (status == 1) {
                star.setImageResource(R.drawable.ic_star_filled);
            } else if (status == -1) {
                star.setImageResource(R.drawable.ic_star_red);
            } else {
                star.setImageResource(R.drawable.ic_star_outline);
            }
            starAccumulationContainer.addView(star);
        }
    }

    private void showConfetti() {
        Party party = new PartyFactory(new Emitter(5, TimeUnit.SECONDS).perSecond(100))
                .spread(360)
                .shapes(Shape.Square.INSTANCE, Shape.Circle.INSTANCE)
                .colors(Arrays.asList(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.CYAN, Color.MAGENTA))
                .position(0.5, 0, 0.5, 0)
                .build();
        konfettiView.start(party);
    }

    private final class MyTouchListener implements View.OnTouchListener {
        public boolean onTouch(View view, android.view.MotionEvent motionEvent) {
            if (isAnimating) {
                return false;
            }
            if (motionEvent.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                if (view.getTag() == null) {
                    return false;
                }
                ClipData.Item item = new ClipData.Item((CharSequence) view.getTag());
                ClipData dragData = new ClipData(
                        (CharSequence) view.getTag(),
                        new String[]{ClipDescription.MIMETYPE_TEXT_PLAIN},
                        item);
                View.DragShadowBuilder myShadow = new View.DragShadowBuilder(view);
                view.startDrag(dragData, myShadow, null, 0);
                return true;
            } else {
                return false;
            }
        }
    }

    class MyDragListener implements View.OnDragListener {
        @Override
        public boolean onDrag(View v, DragEvent event) {
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    return true;
                case DragEvent.ACTION_DRAG_ENTERED:
                    v.setBackgroundColor(0x22000000);
                    return true;
                case DragEvent.ACTION_DRAG_EXITED:
                    v.setBackgroundColor(0x00000000);
                    return true;
                case DragEvent.ACTION_DROP:
                    v.setBackgroundColor(0x00000000);
                    ClipData.Item item = event.getClipData().getItemAt(0);
                    String dragData = item.getText().toString();

                    if (dragData.equals(centerLetter.getText().toString())) {
                        isAnimating = true;
                        if (incorrectAttempts == 0) {
                            levelCompletionStatus.set(currentLevel - 1, 1);
                        }
                        showCustomToast("Well done!", true);
                        updateStarDisplay();
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            if (wellDoneSound != null && wellDoneSound.isPlaying()) {
                                wellDoneSound.pause();
                                wellDoneSound.seekTo(0);
                            }
                            nextLevel();
                        }, 5000);
                    } else {
                        isAnimating = true;
                        levelCompletionStatus.set(currentLevel - 1, -1);
                        incorrectAttempts++;
                        showCustomToast("Oops!", false);
                        updateStarDisplay();
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            nextLevel();
                        }, 2000);
                    }
                    return true;
                case DragEvent.ACTION_DRAG_ENDED:
                    return true;
                default:
                    return false;
            }
        }
    }
}
