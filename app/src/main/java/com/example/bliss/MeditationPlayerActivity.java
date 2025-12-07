package com.example.bliss;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.progressindicator.CircularProgressIndicator;

public class MeditationPlayerActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvMeditationTitle;
    private TextView tvMeditationSubtitle;
    private TextView tvTimer;
    private TextView tvStepProgress;
    private TextView tvPercentage;
    private CircularProgressIndicator circularProgress;
    private Button btnStartPause;

    private CountDownTimer countDownTimer;
    private boolean isPlaying = false;
    private long timeLeftInMillis;
    private long totalTimeInMillis;
    private int currentStep = 1;
    private int totalSteps = 13;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meditation_player);

        // Get data from intent
        String title = getIntent().getStringExtra("title");
        String subtitle = getIntent().getStringExtra("subtitle");
        String duration = getIntent().getStringExtra("duration");

        // Initialize views
        btnBack = findViewById(R.id.btnBack);
        tvMeditationTitle = findViewById(R.id.tvMeditationTitle);
        tvMeditationSubtitle = findViewById(R.id.tvMeditationSubtitle);
        tvTimer = findViewById(R.id.tvTimer);
        tvStepProgress = findViewById(R.id.tvStepProgress);
        tvPercentage = findViewById(R.id.tvPercentage);
        circularProgress = findViewById(R.id.circularProgress);
        btnStartPause = findViewById(R.id.btnStartPause);

        // Set meditation details
        tvMeditationTitle.setText(title != null ? title : "Body Scan");
        tvMeditationSubtitle.setText(subtitle != null ? subtitle : "Progressive relaxation");

        // Parse duration (e.g., "10 min" -> 10 minutes)
        int minutes = 10; // default
        if (duration != null) {
            try {
                minutes = Integer.parseInt(duration.replaceAll("[^0-9]", ""));
            } catch (NumberFormatException e) {
                minutes = 10;
            }
        }

        totalTimeInMillis = minutes * 60 * 1000;
        timeLeftInMillis = totalTimeInMillis;
        updateTimerText();
        updateProgress();

        // Back button click
        btnBack.setOnClickListener(v -> finish());

        // Start/Pause button click
        btnStartPause.setOnClickListener(v -> {
            if (isPlaying) {
                pauseMeditation();
            } else {
                startMeditation();
            }
        });
    }

    private void startMeditation() {
        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateTimerText();
                updateProgress();
            }

            @Override
            public void onFinish() {
                isPlaying = false;
                btnStartPause.setText("Restart");
                timeLeftInMillis = totalTimeInMillis;
                updateTimerText();
                updateProgress();
            }
        }.start();

        isPlaying = true;
        btnStartPause.setText("Pause");
    }

    private void pauseMeditation() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        isPlaying = false;
        btnStartPause.setText("Resume");
    }

    private void updateTimerText() {
        long totalSeconds = totalTimeInMillis / 1000;
        long elapsedSeconds = (totalTimeInMillis - timeLeftInMillis) / 1000;

        int elapsedMinutes = (int) (elapsedSeconds / 60);
        int elapsedSecs = (int) (elapsedSeconds % 60);
        int totalMinutes = (int) (totalSeconds / 60);

        String timeText = String.format("%d:%02d / %d min",
                elapsedMinutes, elapsedSecs, totalMinutes);
        tvTimer.setText(timeText);
    }

    private void updateProgress() {
        int percentage = (int) ((totalTimeInMillis - timeLeftInMillis) * 100 / totalTimeInMillis);
        circularProgress.setProgress(percentage);
        tvPercentage.setText(percentage + " %");

        // Update step based on percentage
        currentStep = Math.max(1, (percentage * totalSteps) / 100);
        tvStepProgress.setText("Step " + currentStep + " of " + totalSteps);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}