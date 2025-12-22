package com.example.music;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.bliss.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class TrackMoodFragment extends Fragment {

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

    public TrackMoodFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 加载布局
        return inflater.inflate(R.layout.fragment_track_mood, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeViews(view);
        setupListeners();

        btnAISuggestion.setEnabled(false);
        btnAISuggestion.setAlpha(0.5f);

        showPreviousMood();
    }

    private void initializeViews(View view) {
        btnHappy = view.findViewById(R.id.btnHappy);
        btnCalm = view.findViewById(R.id.btnCalm);
        btnSad = view.findViewById(R.id.btnSad);
        btnStressed = view.findViewById(R.id.btnStressed);
        btnAISuggestion = view.findViewById(R.id.btnAIMS);
    }

    private void setupListeners() {
        btnHappy.setOnClickListener(v -> selectMood("happy", btnHappy));
        btnCalm.setOnClickListener(v -> selectMood("calm", btnCalm));
        btnSad.setOnClickListener(v -> selectMood("sad", btnSad));
        btnStressed.setOnClickListener(v -> selectMood("stressed", btnStressed));

        btnAISuggestion.setOnClickListener(v -> {
            if (selectedMood.isEmpty()) {
                Toast.makeText(requireContext(), "Please select a mood first", Toast.LENGTH_SHORT).show();
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
        Toast.makeText(requireContext(), "Selected: " + getDisplayName(mood), Toast.LENGTH_SHORT).show();
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
        if (getActivity() == null) return;
        SharedPreferences prefs = getActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String previousMood = prefs.getString(KEY_SELECTED_MOOD, "");
        if (!previousMood.isEmpty()) {
            Snackbar.make(getView(), "Last mood: " + getDisplayName(previousMood), Snackbar.LENGTH_LONG).show();
        }
    }

    private void saveMoodAndNavigate() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();

        Map<String, Object> moodData = new HashMap<>();
        moodData.put("mood", selectedMood.toLowerCase());
        moodData.put("timestamp", FieldValue.serverTimestamp());
        moodData.put("time_millis", System.currentTimeMillis());

        if (getActivity() != null) {
            getActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                    .putString(KEY_SELECTED_MOOD, selectedMood)
                    .apply();
        }

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
                    performNavigation();
                });
    }

    private void performNavigation() {
        if (isAdded()) {
            Intent intent = new Intent(getActivity(), MusicSuggestionActivity.class);
            intent.putExtra("SELECTED_MOOD", selectedMood);
            startActivity(intent);
            getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
    }

    private String getDisplayName(String mood) {
        if (mood == null || mood.isEmpty()) return "";
        return mood.substring(0, 1).toUpperCase() + mood.substring(1).toLowerCase();
    }
}