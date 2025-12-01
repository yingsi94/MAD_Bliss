package com.example.bliss;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.example.bliss.ai.GeminiHelper;
import com.example.bliss.model.JournalEntry;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Date;

public class AddJournalActivity extends AppCompatActivity {

    private EditText etTitle, etContent;
    private Button btnSave, btnCancel, btnAnalyze;
    private ImageButton btnBack;
    private CardView cvAiSuggestion;
    private TextView tvAiSuggestion;
    private android.widget.ScrollView svMain;
    private FirebaseFirestore db;
    private GeminiHelper geminiHelper;
    private String currentMood = "";
    private String currentSuggestion = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_journal);

        etTitle = findViewById(R.id.etTitle);
        etContent = findViewById(R.id.etContent);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
        btnAnalyze = findViewById(R.id.btnAnalyze);
        btnBack = findViewById(R.id.btnBack);
        cvAiSuggestion = findViewById(R.id.cvAiSuggestion);
        tvAiSuggestion = findViewById(R.id.tvAiSuggestion);
        svMain = findViewById(R.id.svMain);

        db = FirebaseFirestore.getInstance();
        geminiHelper = new GeminiHelper();

        btnBack.setOnClickListener(v -> finish());
        btnCancel.setOnClickListener(v -> finish());

        btnAnalyze.setOnClickListener(v -> analyzeEntry());

        btnSave.setOnClickListener(v -> saveEntry());
    }

    private void analyzeEntry() {
        String title = etTitle.getText().toString();
        String content = etContent.getText().toString();

        if (content.isEmpty()) {
            Toast.makeText(this, "Please write something to analyze", Toast.LENGTH_SHORT).show();
            return;
        }

        btnAnalyze.setEnabled(false);
        btnAnalyze.setText("Analyzing...");

        geminiHelper.analyzeJournalEntry(title, content, new GeminiHelper.AnalysisCallback() {
            @Override
            public void onAnalysisSuccess(String result) {
                runOnUiThread(() -> {
                    android.util.Log.d("AddJournalActivity", "Gemini Analysis Success: " + result);
                    btnAnalyze.setEnabled(true);
                    btnAnalyze.setText("Analyze with AI");
                    cvAiSuggestion.setVisibility(View.VISIBLE);
                    tvAiSuggestion.setText(result);
                    
                    // Scroll to the bottom to show the result
                    svMain.post(() -> svMain.fullScroll(View.FOCUS_DOWN));
                    
                    // Robust Parsing Logic
                    String cleanResult = result.replace("*", "").trim(); // Remove markdown
                    
                    int moodIdx = cleanResult.indexOf("Mood:");
                    int suggestionIdx = cleanResult.indexOf("Suggestion:");
                    
                    if (moodIdx != -1 && suggestionIdx != -1 && moodIdx < suggestionIdx) {
                        // Standard format found
                        currentMood = cleanResult.substring(moodIdx + 5, suggestionIdx).trim();
                        currentSuggestion = cleanResult.substring(suggestionIdx + 11).trim();
                    } else if (moodIdx != -1) {
                        // Only mood found
                        currentMood = cleanResult.substring(moodIdx + 5).trim();
                        currentSuggestion = cleanResult; // Keep full text as suggestion
                    } else {
                        // Fallback
                        currentMood = "Neutral";
                        currentSuggestion = cleanResult;
                    }
                });
            }

            @Override
            public void onAnalysisFailure(Throwable t) {
                runOnUiThread(() -> {
                    android.util.Log.e("AddJournalActivity", "Gemini Analysis Failed", t);
                    btnAnalyze.setEnabled(true);
                    btnAnalyze.setText("Analyze with AI");
                    Toast.makeText(AddJournalActivity.this, "Analysis failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void saveEntry() {
        String title = etTitle.getText().toString();
        String content = etContent.getText().toString();

        if (title.isEmpty() || content.isEmpty()) {
            Toast.makeText(this, "Please fill in title and content", Toast.LENGTH_SHORT).show();
            return;
        }
        
        android.util.Log.d("AddJournalActivity", "Saving Entry - Mood: " + currentMood + ", Suggestion: " + currentSuggestion);

        JournalEntry entry = new JournalEntry(title, content, new Timestamp(new Date()), null);
        entry.setMood(currentMood);
        entry.setSuggestion(currentSuggestion);

        db.collection("journals")
                .add(entry)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(AddJournalActivity.this, "Journal saved!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AddJournalActivity.this, "Error saving journal: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    android.util.Log.e("AddJournalActivity", "Error saving journal", e);
                });
    }
}
