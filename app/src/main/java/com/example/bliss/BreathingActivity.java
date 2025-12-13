package com.example.bliss;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import java.util.Locale;

public class BreathingActivity extends AppCompatActivity {

    private static final String TAG = "BreathingActivity";

    private Button btnMeditation, btnBreathing, btnGoals, btnStart;
    private ImageButton btnSettings;
    private TextView tvBreathingAction, tvBreathingTimer, tvCycleCounter;
    private ImageView ivBreathingIcon;
    private CardView breathingCircle;
    private View outerRing1, outerRing2;
    private LinearLayout settingsPanel;
    private NumberPicker npCycles;
    private SwitchCompat switchSound;

    private CountDownTimer breathingTimer;
    private boolean isBreathing = false;
    private int currentCycle = 0;
    private int maxCycles = 4;
    private boolean soundEnabled = true;

    // Breathing phases
    private static final int PHASE_INHALE = 0;
    private static final int PHASE_HOLD = 1;
    private static final int PHASE_EXHALE = 2;
    private int currentPhase = PHASE_INHALE;

    // Text-to-Speech
    private TextToSpeech textToSpeech;
    private boolean ttsReady = false;

    // Animation
    private ValueAnimator circleAnimator;
    private ObjectAnimator ring1Animator, ring2Animator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_breathing);

        // Initialize views
        initializeViews();

        // Setup Text-to-Speech
        setupTextToSpeech();

        // Setup listeners
        setupListeners();

        // Setup NumberPicker
        setupNumberPicker();
    }

    private void initializeViews() {
        btnMeditation = findViewById(R.id.btnMeditation);
        btnBreathing = findViewById(R.id.btnBreathing);
        btnGoals = findViewById(R.id.btnGoals);
        btnStart = findViewById(R.id.btnStart);
        btnSettings = findViewById(R.id.btnSettings);

        tvBreathingAction = findViewById(R.id.tvBreathingAction);
        tvBreathingTimer = findViewById(R.id.tvBreathingTimer);
        tvCycleCounter = findViewById(R.id.tvCycleCounter);
        ivBreathingIcon = findViewById(R.id.ivBreathingIcon);

        breathingCircle = findViewById(R.id.breathingCircle);
        outerRing1 = findViewById(R.id.outerRing1);
        outerRing2 = findViewById(R.id.outerRing2);

        settingsPanel = findViewById(R.id.settingsPanel);
        npCycles = findViewById(R.id.npCycles);
        switchSound = findViewById(R.id.switchSound);

        updateCycleCounter();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Always highlight Breathing tab when this activity is visible
        selectTab(btnBreathing);
    }

    private void selectTab(Button selectedButton) {
        // Reset all tabs
        resetTab(btnMeditation);
        resetTab(btnBreathing);
        resetTab(btnGoals);

        // Highlight selected tab (white background, white text)
        selectedButton.setBackgroundResource(R.drawable.tab_selected_white);
        selectedButton.setTextColor(ContextCompat.getColor(this, android.R.color.white));
    }

    private void resetTab(Button button) {
        // Unselected tabs (transparent background, purple text)
        button.setBackgroundResource(android.R.color.transparent);
        button.setTextColor(ContextCompat.getColor(this, R.color.purple_dark));
    }

    private void setupTextToSpeech() {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.US);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "Language not supported");
                } else {
                    ttsReady = true;
                    textToSpeech.setPitch(1.0f);
                    textToSpeech.setSpeechRate(0.9f);
                }
            } else {
                Log.e(TAG, "TTS initialization failed");
            }
        });
    }

    private void setupNumberPicker() {
        npCycles.setMinValue(1);
        npCycles.setMaxValue(10);
        npCycles.setValue(4);
        npCycles.setWrapSelectorWheel(false);

        npCycles.setOnValueChangedListener((picker, oldVal, newVal) -> {
            maxCycles = newVal;
            updateCycleCounter();
        });
    }

    private void setupListeners() {
        // Tab button listeners
        btnMeditation.setOnClickListener(v -> {
            Intent intent = new Intent(BreathingActivity.this, RelaxationActivity.class);
            startActivity(intent);
        });

        btnBreathing.setOnClickListener(v -> {
            // Already on Breathing tab - do nothing
        });

        btnGoals.setOnClickListener(v -> {
            Intent intent = new Intent(BreathingActivity.this, GoalsActivity.class);
            startActivity(intent);
        });

        // Start button listener
        btnStart.setOnClickListener(v -> {
            if (!isBreathing) {
                startBreathingExercise();
            } else {
                stopBreathingExercise();
            }
        });

        // Settings button listener
        btnSettings.setOnClickListener(v -> {
            if (settingsPanel.getVisibility() == View.VISIBLE) {
                settingsPanel.setVisibility(View.GONE);
            } else {
                settingsPanel.setVisibility(View.VISIBLE);
            }
        });

        // Sound toggle listener
        switchSound.setOnCheckedChangeListener((buttonView, isChecked) -> {
            soundEnabled = isChecked;
        });
    }

    private void startBreathingExercise() {
        isBreathing = true;
        currentCycle = 0;
        currentPhase = PHASE_INHALE;
        btnStart.setText("Stop");
        settingsPanel.setVisibility(View.GONE);
        updateCycleCounter();
        startPhase();
    }

    private void stopBreathingExercise() {
        isBreathing = false;
        if (breathingTimer != null) {
            breathingTimer.cancel();
        }
        cancelAnimations();
        btnStart.setText("Start");
        tvBreathingAction.setText("Ready");
        tvBreathingTimer.setText("Press Start");
        resetCircleAndRings();
        updateCycleCounter();
    }

    private void startPhase() {
        if (!isBreathing) return;

        switch (currentPhase) {
            case PHASE_INHALE:
                tvBreathingAction.setText("Inhale");
                ivBreathingIcon.setImageResource(R.drawable.ic_arrow_up);
                speak("Breathe in");
                startTimer(4, () -> {
                    currentPhase = PHASE_HOLD;
                    startPhase();
                });
                animateCircle(240, 280, 4000);
                animateRings(300, 340, 270, 310, 4000);
                break;

            case PHASE_HOLD:
                tvBreathingAction.setText("Hold");
                ivBreathingIcon.setImageResource(R.drawable.ic_pause);
                speak("Hold");
                startTimer(7, () -> {
                    currentPhase = PHASE_EXHALE;
                    startPhase();
                });
                // Keep circle at current size
                break;

            case PHASE_EXHALE:
                tvBreathingAction.setText("Exhale");
                ivBreathingIcon.setImageResource(R.drawable.ic_arrow_down);
                speak("Breathe out");
                startTimer(8, () -> {
                    currentCycle++;
                    updateCycleCounter();
                    if (currentCycle >= maxCycles) {
                        stopBreathingExercise();
                        Toast.makeText(this, "Great job! Exercise completed", Toast.LENGTH_LONG).show();
                        speak("Breathing exercise completed. Well done!");
                    } else {
                        currentPhase = PHASE_INHALE;
                        startPhase();
                    }
                });
                animateCircle(280, 240, 8000);
                animateRings(340, 300, 310, 270, 8000);
                break;
        }
    }

    private void startTimer(int seconds, Runnable onComplete) {
        if (breathingTimer != null) {
            breathingTimer.cancel();
        }

        breathingTimer = new CountDownTimer(seconds * 1000L, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secondsLeft = (int) (millisUntilFinished / 1000) + 1;
                tvBreathingTimer.setText(secondsLeft + " sec");
            }

            @Override
            public void onFinish() {
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        }.start();
    }

    private void animateCircle(int fromSize, int toSize, long duration) {
        if (circleAnimator != null && circleAnimator.isRunning()) {
            circleAnimator.cancel();
        }

        circleAnimator = ValueAnimator.ofInt(fromSize, toSize);
        circleAnimator.setDuration(duration);
        circleAnimator.setInterpolator(new AccelerateDecelerateInterpolator());

        circleAnimator.addUpdateListener(animation -> {
            int size = (int) animation.getAnimatedValue();
            int sizePx = (int) (size * getResources().getDisplayMetrics().density);

            breathingCircle.getLayoutParams().width = sizePx;
            breathingCircle.getLayoutParams().height = sizePx;
            breathingCircle.setRadius(sizePx / 2f);
            breathingCircle.requestLayout();
        });

        circleAnimator.start();
    }

    private void animateRings(int fromSize1, int toSize1, int fromSize2, int toSize2, long duration) {
        // Animate outer ring 1
        if (ring1Animator != null && ring1Animator.isRunning()) {
            ring1Animator.cancel();
        }

        ring1Animator = ObjectAnimator.ofFloat(outerRing1, "scaleX",
                fromSize1 / 300f, toSize1 / 300f);
        ring1Animator.setDuration(duration);
        ring1Animator.setInterpolator(new AccelerateDecelerateInterpolator());

        ObjectAnimator ring1ScaleY = ObjectAnimator.ofFloat(outerRing1, "scaleY",
                fromSize1 / 300f, toSize1 / 300f);
        ring1ScaleY.setDuration(duration);
        ring1ScaleY.setInterpolator(new AccelerateDecelerateInterpolator());

        // Animate outer ring 2
        if (ring2Animator != null && ring2Animator.isRunning()) {
            ring2Animator.cancel();
        }

        ring2Animator = ObjectAnimator.ofFloat(outerRing2, "scaleX",
                fromSize2 / 270f, toSize2 / 270f);
        ring2Animator.setDuration(duration);
        ring2Animator.setInterpolator(new AccelerateDecelerateInterpolator());

        ObjectAnimator ring2ScaleY = ObjectAnimator.ofFloat(outerRing2, "scaleY",
                fromSize2 / 270f, toSize2 / 270f);
        ring2ScaleY.setDuration(duration);
        ring2ScaleY.setInterpolator(new AccelerateDecelerateInterpolator());

        // Start all animations
        ring1Animator.start();
        ring1ScaleY.start();
        ring2Animator.start();
        ring2ScaleY.start();

        // Animate alpha for pulsing effect
        ObjectAnimator alpha1 = ObjectAnimator.ofFloat(outerRing1, "alpha", 0.3f, 0.6f, 0.3f);
        alpha1.setDuration(duration);
        alpha1.start();

        ObjectAnimator alpha2 = ObjectAnimator.ofFloat(outerRing2, "alpha", 0.4f, 0.7f, 0.4f);
        alpha2.setDuration(duration);
        alpha2.start();
    }

    private void cancelAnimations() {
        if (circleAnimator != null && circleAnimator.isRunning()) {
            circleAnimator.cancel();
        }
        if (ring1Animator != null && ring1Animator.isRunning()) {
            ring1Animator.cancel();
        }
        if (ring2Animator != null && ring2Animator.isRunning()) {
            ring2Animator.cancel();
        }
    }

    private void resetCircleAndRings() {
        int defaultSize = 240;
        int sizePx = (int) (defaultSize * getResources().getDisplayMetrics().density);
        breathingCircle.getLayoutParams().width = sizePx;
        breathingCircle.getLayoutParams().height = sizePx;
        breathingCircle.setRadius(sizePx / 2f);
        breathingCircle.requestLayout();

        outerRing1.setScaleX(1.0f);
        outerRing1.setScaleY(1.0f);
        outerRing1.setAlpha(0.3f);

        outerRing2.setScaleX(1.0f);
        outerRing2.setScaleY(1.0f);
        outerRing2.setAlpha(0.4f);

        ivBreathingIcon.setImageResource(R.drawable.ic_lungs);
    }

    private void updateCycleCounter() {
        tvCycleCounter.setText("Cycle " + currentCycle + " / " + maxCycles);
    }

    private void speak(String text) {
        if (soundEnabled && ttsReady && textToSpeech != null) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (breathingTimer != null) {
            breathingTimer.cancel();
        }
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        cancelAnimations();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isBreathing) {
            stopBreathingExercise();
        }
    }
}