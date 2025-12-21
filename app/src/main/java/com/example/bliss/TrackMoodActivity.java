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

    // 视图变量
    private MaterialButton btnHappy, btnCalm, btnSad, btnStressed;
    private MaterialButton btnAISuggestion;
    private String selectedMood = "";

    // 本地存储常量
    private static final String PREFS_NAME = "MoodPrefs";
    private static final String KEY_SELECTED_MOOD = "selected_mood";

    // 颜色常量
    private static final int COLOR_HAPPY = Color.parseColor("#8979FF");
    private static final int COLOR_CALM = Color.parseColor("#FF928A");
    private static final int COLOR_SAD = Color.parseColor("#3CC3DF");
    private static final int COLOR_STRESSED = Color.parseColor("#FFAE4C");

    // Firebase 实例
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_mood);

        // 初始化 Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeViews();
        setupListeners();

        // 初始状态：禁用 AI 按钮直到用户选择心情
        btnAISuggestion.setEnabled(false);
        btnAISuggestion.setAlpha(0.5f);

        showPreviousMood(); // 显示上次记录的心情（从本地获取）
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

        // AI 建议按钮点击事件
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
            Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG);
            snackbar.setAction("USE THIS", v -> {
                MaterialButton button = getButtonForMood(previousMood);
                if (button != null) selectMood(previousMood, button);
            });
            snackbar.show();
        }
    }

    /**
     * 核心方法：保存心情到 Firestore (方案 A) 并跳转
     */
    private void saveMoodAndNavigate() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Login error. Please sign in again.", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();

        // 1. 准备数据
        Map<String, Object> moodData = new HashMap<>();
        moodData.put("mood", selectedMood);
        moodData.put("timestamp", FieldValue.serverTimestamp());

        // 2. 存储到本地 SharedPreferences (快速读取)
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putString(KEY_SELECTED_MOOD, selectedMood);
        editor.apply();

        // 3. 存储到 Firestore (方案 A: 子集合记录历史)
        // 路径: users -> {userId} -> mood_history -> {随机文档ID}
        db.collection("users")
                .document(userId)
                .collection("mood_history")
                .add(moodData)
                .addOnSuccessListener(documentReference -> {
                    Log.d("FIRESTORE", "History saved: " + documentReference.getId());
                    performNavigation();
                })
                .addOnFailureListener(e -> {
                    Log.e("FIRESTORE", "Save failed", e);
                    // 即使网络失败，我们也允许跳转，确保用户体验
                    performNavigation();
                });
    }

    private void performNavigation() {
        try {
            Intent intent = new Intent(TrackMoodActivity.this, MusicSuggestionActivity.class);
            // 传递当前选中的心情给下一个页面，方便 AI 直接生成建议
            intent.putExtra("SELECTED_MOOD", selectedMood);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        } catch (Exception e) {
            Log.e("NAVIGATION", "Error: " + e.getMessage());
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
        if (mood == null) return "";
        return mood.substring(0, 1).toUpperCase() + mood.substring(1);
    }
}