package com.example.bliss;

import android.animation.ValueAnimator;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class BreathingActivity extends AppCompatActivity {

    private Button btnMeditation, btnBreathing, btnGoals, btnStart;
    private TextView tvBreathingAction, tvBreathingTimer;
    private CardView breathingCircle;
    private CountDownTimer breathingTimer;
    private boolean isBreathing = false;
    private int currentCycle = 0;
    private int maxCycles = 4;

    // Breathing phases
    private static final int PHASE_INHALE = 0;
    private static final int PHASE_HOLD = 1;
    private static final int PHASE_EXHALE = 2;
    private int currentPhase = PHASE_INHALE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_breathing);

        // Initialize views
        btnMeditation = findViewById(R.id.btnMeditation);
        btnBreathing = findViewById(R.id.btnBreathing);
        btnGoals = findViewById(R.id.btnGoals);
        btnStart = findViewById(R.id.btnStart);
        tvBreathingAction = findViewById(R.id.tvBreathingAction);
        tvBreathingTimer = findViewById(R.id.tvBreathingTimer);
        breathingCircle = findViewById(R.id.breathingCircle);

        // Tab button listeners
        btnMeditation.setOnClickListener(v -> {
            finish(); // Go back to RelaxationActivity (Meditation tab)
        });

        btnBreathing.setOnClickListener(v -> {
            // Already on Breathing tab
        });

        btnGoals.setOnClickListener(v -> {
            Toast.makeText(this, "Goals coming soon", Toast.LENGTH_SHORT).show();
        });

        // Start button listener
        btnStart.setOnClickListener(v -> {
            if (!isBreathing) {
                startBreathingExercise();
            } else {
                stopBreathingExercise();
            }
        });
    }

    private void startBreathingExercise() {
        isBreathing = true;
        currentCycle = 0;
        currentPhase = PHASE_INHALE;
        btnStart.setText("Stop");
        startPhase();
    }

    private void stopBreathingExercise() {
        isBreathing = false;
        if (breathingTimer != null) {
            breathingTimer.cancel();
        }
        btnStart.setText("Start");
        tvBreathingAction.setText("Inhale");
        tvBreathingTimer.setText("4 seconds");
        resetCircleSize();
    }

    private void startPhase() {
        if (!isBreathing) return;

        switch (currentPhase) {
            case PHASE_INHALE:
                tvBreathingAction.setText("Inhale");
                startTimer(4, () -> {
                    currentPhase = PHASE_HOLD;
                    startPhase();
                });
                animateCircle(240, 300, 4000); // Expand
                break;

            case PHASE_HOLD:
                tvBreathingAction.setText("Hold");
                startTimer(7, () -> {
                    currentPhase = PHASE_EXHALE;
                    startPhase();
                });
                // Keep circle at current size
                break;

            case PHASE_EXHALE:
                tvBreathingAction.setText("Exhale");
                startTimer(8, () -> {
                    currentCycle++;
                    if (currentCycle >= maxCycles) {
                        stopBreathingExercise();
                        Toast.makeText(this, "Breathing exercise completed!", Toast.LENGTH_SHORT).show();
                    } else {
                        currentPhase = PHASE_INHALE;
                        startPhase();
                    }
                });
                animateCircle(300, 240, 8000); // Shrink
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
                tvBreathingTimer.setText(secondsLeft + " seconds");
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
        ValueAnimator animator = ValueAnimator.ofInt(fromSize, toSize);
        animator.setDuration(duration);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());

        animator.addUpdateListener(animation -> {
            int size = (int) animation.getAnimatedValue();
            int sizePx = (int) (size * getResources().getDisplayMetrics().density);

            breathingCircle.getLayoutParams().width = sizePx;
            breathingCircle.getLayoutParams().height = sizePx;
            breathingCircle.requestLayout();
        });

        animator.start();
    }

    private void resetCircleSize() {
        int defaultSize = 240;
        int sizePx = (int) (defaultSize * getResources().getDisplayMetrics().density);
        breathingCircle.getLayoutParams().width = sizePx;
        breathingCircle.getLayoutParams().height = sizePx;
        breathingCircle.requestLayout();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (breathingTimer != null) {
            breathingTimer.cancel();
        }
    }
}