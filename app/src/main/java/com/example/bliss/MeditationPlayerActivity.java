package com.example.bliss;

import android.animation.ObjectAnimator;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

public class MeditationPlayerActivity extends AppCompatActivity {

    private static final String TAG = "MeditationPlayer";

    // UI Components
    private ImageButton btnBack, btnPrevious, btnNext;
    private TextView tvThemeName, tvMusicName, tvTimer, tvThemeText, tvTextProgress, tvPercentage;
    private TextView tvBreathingInstruction;
    private CircularProgressIndicator circularProgress;
    private Button btnStartPause;
    private View breathingCircle;
    private LinearLayout volumeControl;
    private SeekBar seekBarVolume;

    // Data from Intent
    private int totalDurationSeconds;
    private String musicUrl;
    private String musicName;
    private String themeName;
    private ArrayList<String> themeTexts;
    private boolean breathingGuideEnabled;

    // Meditation State
    private CountDownTimer countDownTimer;
    private boolean isPlaying = false;
    private long timeLeftInMillis;
    private long totalTimeInMillis;
    private int currentTextIndex = 0;

    // Media Players
    private MediaPlayer backgroundMusicPlayer;
    private TextToSpeech textToSpeech;
    private boolean ttsReady = false;

    // Breathing Guide (4-7-8 pattern)
    private Handler breathingHandler;
    private Runnable breathingRunnable;
    private int breathingPhase = 0; // 0=inhale(4s), 1=hold(7s), 2=exhale(8s)
    private ObjectAnimator breathingAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meditation_player);

        // Get data from intent
        getIntentData();

        // Initialize views
        initializeViews();

        // Setup listeners
        setupListeners();

        // Setup breathing guide if enabled
        if (breathingGuideEnabled) {
            setupBreathingGuide();
        }

        // Setup background music if provided
        if (musicUrl != null && !musicUrl.isEmpty()) {
            setupBackgroundMusic();
        }

        // Setup Text-to-Speech for breathing instructions
        setupTextToSpeech();

        // Display first theme text
        updateThemeText();
        updateProgress();
    }

    private void getIntentData() {
        totalDurationSeconds = getIntent().getIntExtra("duration", 600); // default 10 min
        musicUrl = getIntent().getStringExtra("musicUrl");
        musicName = getIntent().getStringExtra("musicName");
        themeName = getIntent().getStringExtra("themeName");
        themeTexts = getIntent().getStringArrayListExtra("themeTexts");
        breathingGuideEnabled = getIntent().getBooleanExtra("breathingGuide", false);

        if (themeTexts == null || themeTexts.isEmpty()) {
            themeTexts = new ArrayList<>();
            themeTexts.add("Focus on your breath");
            themeTexts.add("Let your body relax");
            themeTexts.add("Be present in this moment");
        }

        totalTimeInMillis = totalDurationSeconds * 1000L;
        timeLeftInMillis = totalTimeInMillis;
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnNext = findViewById(R.id.btnNext);
        btnStartPause = findViewById(R.id.btnStartPause);

        tvThemeName = findViewById(R.id.tvThemeName);
        tvMusicName = findViewById(R.id.tvMusicName);
        tvTimer = findViewById(R.id.tvTimer);
        tvThemeText = findViewById(R.id.tvThemeText);
        tvTextProgress = findViewById(R.id.tvTextProgress);
        tvPercentage = findViewById(R.id.tvPercentage);
        tvBreathingInstruction = findViewById(R.id.tvBreathingInstruction);

        circularProgress = findViewById(R.id.circularProgress);
        breathingCircle = findViewById(R.id.breathingCircle);
        volumeControl = findViewById(R.id.volumeControl);
        seekBarVolume = findViewById(R.id.seekBarVolume);

        // Set theme name
        tvThemeName.setText(themeName != null ? themeName : "Meditation");

        // Show music name if music is playing
        if (musicName != null && !musicName.equals("No Music")) {
            tvMusicName.setText("♫ " + musicName);
            tvMusicName.setVisibility(View.VISIBLE);
            volumeControl.setVisibility(View.VISIBLE);
        }

        // Show breathing instruction if enabled
        if (breathingGuideEnabled) {
            tvBreathingInstruction.setVisibility(View.VISIBLE);
            breathingCircle.setVisibility(View.VISIBLE);
        }

        updateTimerText();
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnStartPause.setOnClickListener(v -> {
            if (isPlaying) {
                pauseMeditation();
            } else {
                startMeditation();
            }
        });

        btnPrevious.setOnClickListener(v -> {
            if (currentTextIndex > 0) {
                currentTextIndex--;
                updateThemeText();
            }
        });

        btnNext.setOnClickListener(v -> {
            if (currentTextIndex < themeTexts.size() - 1) {
                currentTextIndex++;
                updateThemeText();
            }
        });

        seekBarVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (backgroundMusicPlayer != null && fromUser) {
                    float volume = progress / 100f;
                    backgroundMusicPlayer.setVolume(volume, volume);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void setupBackgroundMusic() {
        try {
            backgroundMusicPlayer = new MediaPlayer();
            backgroundMusicPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
            );

            backgroundMusicPlayer.setDataSource(musicUrl);
            backgroundMusicPlayer.setLooping(true);
            backgroundMusicPlayer.prepareAsync();

            backgroundMusicPlayer.setOnPreparedListener(mp -> {
                Log.d(TAG, "Background music prepared");
                float volume = seekBarVolume.getProgress() / 100f;
                mp.setVolume(volume, volume);
            });

            backgroundMusicPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "Music player error: " + what + ", " + extra);
                Toast.makeText(this, "Failed to load background music", Toast.LENGTH_SHORT).show();
                return true;
            });

        } catch (IOException e) {
            Log.e(TAG, "Error setting up background music", e);
            Toast.makeText(this, "Failed to load background music", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupTextToSpeech() {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.US);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "Language not supported");
                } else {
                    ttsReady = true;
                }
            } else {
                Log.e(TAG, "TTS initialization failed");
            }
        });
    }

    private void setupBreathingGuide() {
        breathingHandler = new Handler();
        breathingRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isPlaying) return;

                switch (breathingPhase) {
                    case 0: // Inhale - 4 seconds
                        tvBreathingInstruction.setText("Breathe In");
                        speak("Breathe in");
                        animateBreathingCircle(200, 280, 4000);
                        breathingHandler.postDelayed(this, 4000);
                        breathingPhase = 1;
                        break;

                    case 1: // Hold - 7 seconds
                        tvBreathingInstruction.setText("Hold");
                        speak("Hold");
                        breathingHandler.postDelayed(this, 7000);
                        breathingPhase = 2;
                        break;

                    case 2: // Exhale - 8 seconds
                        tvBreathingInstruction.setText("Breathe Out");
                        speak("Breathe out");
                        animateBreathingCircle(280, 200, 8000);
                        breathingHandler.postDelayed(this, 8000);
                        breathingPhase = 0;
                        break;
                }
            }
        };
    }

    private void animateBreathingCircle(int fromSize, int toSize, long duration) {
        if (breathingAnimator != null && breathingAnimator.isRunning()) {
            breathingAnimator.cancel();
        }

        breathingAnimator = ObjectAnimator.ofFloat(breathingCircle, "scaleX",
                fromSize / 200f, toSize / 200f);
        breathingAnimator.setDuration(duration);
        breathingAnimator.setInterpolator(new AccelerateDecelerateInterpolator());

        ObjectAnimator scaleY = ObjectAnimator.ofFloat(breathingCircle, "scaleY",
                fromSize / 200f, toSize / 200f);
        scaleY.setDuration(duration);
        scaleY.setInterpolator(new AccelerateDecelerateInterpolator());

        breathingAnimator.start();
        scaleY.start();
    }

    private void speak(String text) {
        if (ttsReady && textToSpeech != null) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    private void startMeditation() {
        // Start countdown timer
        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateTimerText();
                updateProgress();

                // Auto-advance theme text based on time
                autoAdvanceThemeText();
            }

            @Override
            public void onFinish() {
                finishMeditation();
            }
        }.start();

        // Start background music if available
        if (backgroundMusicPlayer != null && !backgroundMusicPlayer.isPlaying()) {
            backgroundMusicPlayer.start();
        }

        // Start breathing guide if enabled
        if (breathingGuideEnabled && breathingHandler != null) {
            breathingPhase = 0;
            breathingHandler.post(breathingRunnable);
        }

        isPlaying = true;
        btnStartPause.setText("Pause");
    }

    private void pauseMeditation() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        if (backgroundMusicPlayer != null && backgroundMusicPlayer.isPlaying()) {
            backgroundMusicPlayer.pause();
        }

        if (breathingHandler != null) {
            breathingHandler.removeCallbacks(breathingRunnable);
        }

        if (breathingAnimator != null) {
            breathingAnimator.cancel();
        }

        isPlaying = false;
        btnStartPause.setText("Resume");
    }

    private void finishMeditation() {
        isPlaying = false;
        btnStartPause.setText("Completed");
        btnStartPause.setEnabled(false);

        if (backgroundMusicPlayer != null && backgroundMusicPlayer.isPlaying()) {
            backgroundMusicPlayer.pause();
        }

        Toast.makeText(this, "Meditation completed! Well done.", Toast.LENGTH_LONG).show();
    }

    private void autoAdvanceThemeText() {
        // Calculate which text should be displayed based on elapsed time
        long elapsedTime = totalTimeInMillis - timeLeftInMillis;
        int textInterval = (int) (totalTimeInMillis / themeTexts.size());
        int calculatedIndex = (int) (elapsedTime / textInterval);

        if (calculatedIndex != currentTextIndex && calculatedIndex < themeTexts.size()) {
            currentTextIndex = calculatedIndex;
            updateThemeText();
        }
    }

    private void updateThemeText() {
        if (currentTextIndex < themeTexts.size()) {
            tvThemeText.setText(themeTexts.get(currentTextIndex));
            tvTextProgress.setText("Text " + (currentTextIndex + 1) + " of " + themeTexts.size());
        }
    }

    private void updateTimerText() {
        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;
        String timeText = String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
        tvTimer.setText(timeText);
    }

    private void updateProgress() {
        int percentage = (int) ((totalTimeInMillis - timeLeftInMillis) * 100 / totalTimeInMillis);
        circularProgress.setProgress(percentage);
        tvPercentage.setText(percentage + "%");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        if (backgroundMusicPlayer != null) {
            if (backgroundMusicPlayer.isPlaying()) {
                backgroundMusicPlayer.stop();
            }
            backgroundMusicPlayer.release();
            backgroundMusicPlayer = null;
        }

        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }

        if (breathingHandler != null) {
            breathingHandler.removeCallbacks(breathingRunnable);
        }

        if (breathingAnimator != null) {
            breathingAnimator.cancel();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isPlaying) {
            pauseMeditation();
        }
    }
}