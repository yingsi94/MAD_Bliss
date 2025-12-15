package com.example.bliss;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

public class TrackMoodActivity extends AppCompatActivity {

    private MaterialButton btnHappy, btnCalm, btnSad, btnStressed;
    private MaterialButton btnAISuggestion;
    private String selectedMood = "";

    private static final String PREFS_NAME = "MoodPrefs";
    private static final String KEY_SELECTED_MOOD = "selected_mood";

    private static final int COLOR_HAPPY = Color.parseColor("#8979FF");
    private static final int COLOR_CALM = Color.parseColor("#FF928A");
    private static final int COLOR_SAD = Color.parseColor("#3CC3DF");
    private static final int COLOR_STRESSED = Color.parseColor("#FFAE4C");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_mood); // 确保 R.layout.activity_track_mood 存在

        initializeViews();
        setupListeners();

        btnAISuggestion.setEnabled(false);
        btnAISuggestion.setAlpha(0.5f);

        showPreviousMood();
    }

    private void initializeViews() {
        btnHappy = findViewById(R.id.btnHappy);
        btnCalm = findViewById(R.id.btnCalm);
        btnSad = findViewById(R.id.btnSad);
        btnStressed = findViewById(R.id.btnStressed);
        btnAISuggestion = findViewById(R.id.btnAIMS);

        Log.d("DEBUG", "btnAISuggestion found: " + (btnAISuggestion != null));
    }

    private void setupListeners() {
        // 心情按钮点击事件
        btnHappy.setOnClickListener(v -> selectMood("happy", btnHappy));
        btnCalm.setOnClickListener(v -> selectMood("calm", btnCalm));
        btnSad.setOnClickListener(v -> selectMood("sad", btnSad));
        btnStressed.setOnClickListener(v -> selectMood("stressed", btnStressed));

        // AI Music Suggestion 按钮点击事件
        btnAISuggestion.setOnClickListener(v -> {
            if (selectedMood.isEmpty()) {
                Toast.makeText(this, "Please select a mood first", Toast.LENGTH_SHORT).show();
            } else {
                saveMoodAndNavigate();
            }
        });
    }

    private void selectMood(String mood, MaterialButton button) {
        resetButtons();
        highlightButton(button, getMoodColor(mood));
        selectedMood = mood;
        enableAISuggestion();

        Toast.makeText(this, "Selected: " + getDisplayName(mood), Toast.LENGTH_SHORT).show();
    }

    private void resetButtons() {
        MaterialButton[] buttons = {btnHappy, btnCalm, btnSad, btnStressed};
        for (MaterialButton btn : buttons) {
            btn.setBackgroundColor(Color.TRANSPARENT);
            btn.setTextColor(Color.BLACK);
        }
    }

    private void highlightButton(MaterialButton button, int color) {
        button.setBackgroundColor(color);
        button.setTextColor(Color.WHITE);
    }

    private int getMoodColor(String mood) {
        switch (mood) {
            case "happy": return COLOR_HAPPY;
            case "calm": return COLOR_CALM;
            case "sad": return COLOR_SAD;
            case "stressed": return COLOR_STRESSED;
            default: return COLOR_HAPPY;
        }
    }

    private void enableAISuggestion() {
        btnAISuggestion.setEnabled(true);
        btnAISuggestion.setAlpha(1.0f);
    }

    private void showPreviousMood() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String previousMood = prefs.getString(KEY_SELECTED_MOOD, "");

        if (!previousMood.isEmpty()) {
            String message = "Your last mood was: " + getDisplayName(previousMood);

            Snackbar snackbar = Snackbar.make(
                    findViewById(android.R.id.content),
                    message,
                    Snackbar.LENGTH_LONG
            );

            snackbar.setAction("USE THIS MOOD", v -> {
                MaterialButton button = getButtonForMood(previousMood);
                if (button != null) {
                    selectMood(previousMood, button);
                }
            });

            snackbar.show();
        }
    }

    private MaterialButton getButtonForMood(String mood) {
        switch (mood) {
            case "happy": return btnHappy;
            case "calm": return btnCalm;
            case "sad": return btnSad;
            case "stressed": return btnStressed;
            default: return null;
        }
    }

    private String getDisplayName(String mood) {
        switch (mood) {
            case "happy": return "Happy";
            case "calm": return "Calm";
            case "sad": return "Sad";
            case "stressed": return "Stressed";
            default: return "";
        }
    }

    private void saveMoodAndNavigate() {
        // 保存心情到 SharedPreferences
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_SELECTED_MOOD, selectedMood);
        editor.apply();

        try {
            // 跳转到 MusicSuggestionActivity
            Intent intent = new Intent(TrackMoodActivity.this, MusicSuggestionActivity.class);
            startActivity(intent);

            // 添加转场动画
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        } catch (Exception e) {
            Log.e("NAVIGATION", "Error starting activity: " + e.getMessage());
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}