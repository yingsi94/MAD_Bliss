package com.example.relaxation;

import android.animation.ObjectAnimator;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.bliss.R;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

public class MeditationPlayerFragment extends Fragment {

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

    // Data
    private int totalDurationSeconds;
    private String musicUrl, musicName, themeName;
    private ArrayList<String> themeTexts;
    private boolean breathingGuideEnabled;

    // State
    private CountDownTimer countDownTimer;
    private boolean isPlaying = false;
    private long timeLeftInMillis, totalTimeInMillis;
    private int currentTextIndex = 0;

    // Media
    private MediaPlayer backgroundMusicPlayer;
    private TextToSpeech textToSpeech;
    private boolean ttsReady = false;

    // Breathing Handler
    private Handler breathingHandler;
    private Runnable breathingRunnable;
    private int breathingPhase = 0;
    private ObjectAnimator breathingAnimator;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_meditation_player, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get data from arguments instead of Intent
        getArgumentsData();
        initializeViews(view);
        setupListeners();

        if (breathingGuideEnabled) setupBreathingGuide();
        if (musicUrl != null && !musicUrl.isEmpty()) setupBackgroundMusic();
        setupTextToSpeech();

        updateThemeText();
        updateProgress();
    }

    private void getArgumentsData() {
        Bundle args = getArguments();
        if (args != null) {
            totalDurationSeconds = args.getInt("duration", 600);
            musicUrl = args.getString("musicUrl");
            musicName = args.getString("musicName");
            themeName = args.getString("themeName");
            themeTexts = args.getStringArrayList("themeTexts");
            breathingGuideEnabled = args.getBoolean("breathingGuide", false);
        }

        if (themeTexts == null || themeTexts.isEmpty()) {
            themeTexts = new ArrayList<>();
            themeTexts.add("Focus on your breath");
            themeTexts.add("Let your body relax");
            themeTexts.add("Be present in this moment");
        }

        totalTimeInMillis = totalDurationSeconds * 1000L;
        timeLeftInMillis = totalTimeInMillis;
    }

    private void initializeViews(View view) {
        btnBack = view.findViewById(R.id.btnBack);
        btnPrevious = view.findViewById(R.id.btnPrevious);
        btnNext = view.findViewById(R.id.btnNext);
        btnStartPause = view.findViewById(R.id.btnStartPause);
        tvThemeName = view.findViewById(R.id.tvThemeName);
        tvMusicName = view.findViewById(R.id.tvMusicName);
        tvTimer = view.findViewById(R.id.tvTimer);
        tvThemeText = view.findViewById(R.id.tvThemeText);
        tvTextProgress = view.findViewById(R.id.tvTextProgress);
        tvPercentage = view.findViewById(R.id.tvPercentage);
        tvBreathingInstruction = view.findViewById(R.id.tvBreathingInstruction);
        circularProgress = view.findViewById(R.id.circularProgress);
        breathingCircle = view.findViewById(R.id.breathingCircle);
        volumeControl = view.findViewById(R.id.volumeControl);
        seekBarVolume = view.findViewById(R.id.seekBarVolume);

        tvThemeName.setText(themeName != null ? themeName : "Meditation");
        if (musicName != null && !musicName.equals("No Music")) {
            tvMusicName.setText("♫ " + musicName);
            tvMusicName.setVisibility(View.VISIBLE);
            volumeControl.setVisibility(View.VISIBLE);
        }
        if (breathingGuideEnabled) {
            tvBreathingInstruction.setVisibility(View.VISIBLE);
            breathingCircle.setVisibility(View.VISIBLE);
        }
        updateTimerText();
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> requireActivity().onBackPressed());
        btnStartPause.setOnClickListener(v -> {
            if (isPlaying) pauseMeditation(); else startMeditation();
        });
        btnPrevious.setOnClickListener(v -> {
            if (currentTextIndex > 0) { currentTextIndex--; updateThemeText(); }
        });
        btnNext.setOnClickListener(v -> {
            if (currentTextIndex < themeTexts.size() - 1) { currentTextIndex++; updateThemeText(); }
        });
        seekBarVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (backgroundMusicPlayer != null && fromUser) {
                    float vol = progress / 100f; backgroundMusicPlayer.setVolume(vol, vol);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void setupBackgroundMusic() {
        try {
            backgroundMusicPlayer = new MediaPlayer();
            backgroundMusicPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA).build());
            backgroundMusicPlayer.setDataSource(musicUrl);
            backgroundMusicPlayer.setLooping(true);
            backgroundMusicPlayer.prepareAsync();
            backgroundMusicPlayer.setOnPreparedListener(mp -> {
                float vol = seekBarVolume.getProgress() / 100f; mp.setVolume(vol, vol);
            });
        } catch (IOException e) {
            Log.e(TAG, "Error setting up background music", e);
        }
    }

    private void setupTextToSpeech() {
        textToSpeech = new TextToSpeech(requireContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(Locale.US);
                ttsReady = true;
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
                    case 0: // Inhale
                        tvBreathingInstruction.setText("Breathe In"); speak("Breathe in");
                        animateBreathingCircle(200, 280, 4000);
                        breathingHandler.postDelayed(this, 4000); breathingPhase = 1; break;
                    case 1: // Hold
                        tvBreathingInstruction.setText("Hold"); speak("Hold");
                        breathingHandler.postDelayed(this, 7000); breathingPhase = 2; break;
                    case 2: // Exhale
                        tvBreathingInstruction.setText("Breathe Out"); speak("Breathe out");
                        animateBreathingCircle(280, 200, 8000);
                        breathingHandler.postDelayed(this, 8000); breathingPhase = 0; break;
                }
            }
        };
    }

    private void animateBreathingCircle(int fromSize, int toSize, long duration) {
        if (breathingAnimator != null && breathingAnimator.isRunning()) breathingAnimator.cancel();
        breathingAnimator = ObjectAnimator.ofFloat(breathingCircle, "scaleX", fromSize / 200f, toSize / 200f);
        breathingAnimator.setDuration(duration);
        breathingAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(breathingCircle, "scaleY", fromSize / 200f, toSize / 200f);
        scaleY.setDuration(duration);
        scaleY.setInterpolator(new AccelerateDecelerateInterpolator());
        breathingAnimator.start(); scaleY.start();
    }

    private void speak(String text) {
        if (ttsReady && textToSpeech != null) {
            Bundle params = new Bundle();
            params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 0.4f);
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, params, null);
        }
    }

    private void startMeditation() {
        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished; updateTimerText(); updateProgress(); autoAdvanceThemeText();
            }
            @Override public void onFinish() { finishMeditation(); }
        }.start();

        if (backgroundMusicPlayer != null && !backgroundMusicPlayer.isPlaying()) backgroundMusicPlayer.start();
        if (breathingGuideEnabled && breathingHandler != null) {
            breathingPhase = 0; breathingHandler.post(breathingRunnable);
        }
        isPlaying = true; btnStartPause.setText("Pause");
    }

    private void pauseMeditation() {
        if (countDownTimer != null) countDownTimer.cancel();
        if (backgroundMusicPlayer != null && backgroundMusicPlayer.isPlaying()) backgroundMusicPlayer.pause();
        if (breathingHandler != null) breathingHandler.removeCallbacks(breathingRunnable);
        if (breathingAnimator != null) breathingAnimator.cancel();
        isPlaying = false; btnStartPause.setText("Resume");
    }

    private void finishMeditation() {
        isPlaying = false; btnStartPause.setText("Completed"); btnStartPause.setEnabled(false);
        if (backgroundMusicPlayer != null && backgroundMusicPlayer.isPlaying()) backgroundMusicPlayer.pause();
        Toast.makeText(requireContext(), "Meditation completed! Well done.", Toast.LENGTH_LONG).show();
    }

    private void autoAdvanceThemeText() {
        long elapsedTime = totalTimeInMillis - timeLeftInMillis;
        int textInterval = (int) (totalTimeInMillis / themeTexts.size());
        int calculatedIndex = (int) (elapsedTime / textInterval);
        if (calculatedIndex != currentTextIndex && calculatedIndex < themeTexts.size()) {
            currentTextIndex = calculatedIndex; updateThemeText();
        }
    }

    private void updateThemeText() {
        if (currentTextIndex < themeTexts.size()) {
            tvThemeText.setText(themeTexts.get(currentTextIndex));
            tvTextProgress.setText("Text " + (currentTextIndex + 1) + " of " + themeTexts.size());
        }
    }

    private void updateTimerText() {
        int min = (int) (timeLeftInMillis / 1000) / 60;
        int sec = (int) (timeLeftInMillis / 1000) % 60;
        tvTimer.setText(String.format(Locale.getDefault(), "%d:%02d", min, sec));
    }

    private void updateProgress() {
        int per = (int) ((totalTimeInMillis - timeLeftInMillis) * 100 / totalTimeInMillis);
        circularProgress.setProgress(per);
        tvPercentage.setText(per + "%");
    }

    @Override
    public void onPause() {
        super.onPause();
        if (isPlaying) pauseMeditation();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
        if (backgroundMusicPlayer != null) {
            backgroundMusicPlayer.release(); backgroundMusicPlayer = null;
        }
        if (textToSpeech != null) {
            textToSpeech.stop(); textToSpeech.shutdown();
        }
        if (breathingHandler != null) breathingHandler.removeCallbacks(breathingRunnable);
    }
}