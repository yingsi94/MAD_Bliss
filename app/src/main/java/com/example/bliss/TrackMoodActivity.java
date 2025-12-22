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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

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

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_mood);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

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
    }

    private void setupListeners() {
        btnHappy.setOnClickListener(v -> selectMood("happy", btnHappy));
        btnCalm.setOnClickListener(v -> selectMood("calm", btnCalm));
        btnSad.setOnClickListener(v -> selectMood("sad", btnSad));
        btnStressed.setOnClickListener(v -> selectMood("stressed", btnStressed));

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
            Snackbar.make(findViewById(android.R.id.content), "Last mood: " + getDisplayName(previousMood), Snackbar.LENGTH_LONG).show();
        }
    }

    /**
     * 核心保存逻辑
     */
    private void saveMoodAndNavigate() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();

        // 1. 准备数据
        Map<String, Object> moodData = new HashMap<>();
        moodData.put("mood", selectedMood.toLowerCase());
        moodData.put("timestamp", FieldValue.serverTimestamp()); // Firebase 服务器时间
        moodData.put("time_millis", System.currentTimeMillis()); // 本地毫秒值，方便查询统计

        // 2. 本地持久化 (供 MusicSuggestionActivity 立即使用)
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                .putString(KEY_SELECTED_MOOD, selectedMood)
                .apply();

        // 3. 存储到 Firestore (使用 .add() 确保每次都是新记录)
        db.collection("users")
                .document(userId)
                .collection("mood_history")
                .add(moodData)
                .addOnSuccessListener(docRef -> {
                    Log.d("FIRESTORE", "Mood logged with ID: " + docRef.getId());
                    performNavigation();
                })
                .addOnFailureListener(e -> {
                    Log.e("FIRESTORE", "Save failed", e);
                    performNavigation(); // 即使失败也跳转，防止用户卡住
                });
    }

    private void performNavigation() {
        Intent intent = new Intent(TrackMoodActivity.this, MusicSuggestionActivity.class);
        intent.putExtra("SELECTED_MOOD", selectedMood);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private String getDisplayName(String mood) {
        if (mood == null || mood.isEmpty()) return "";
        return mood.substring(0, 1).toUpperCase() + mood.substring(1).toLowerCase();
    }

    private MaterialButton getButtonForMood(String mood) {
        switch (mood.toLowerCase()) {
            case "happy": return btnHappy;
            case "calm": return btnCalm;
            case "sad": return btnSad;
            case "stressed": return btnStressed;
            default: return null;
        }
    }
}