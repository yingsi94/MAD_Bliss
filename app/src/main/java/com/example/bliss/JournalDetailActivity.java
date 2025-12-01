package com.example.bliss;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.example.bliss.model.JournalEntry;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class JournalDetailActivity extends AppCompatActivity {

    private TextView tvDate, tvTitle, tvContent, tvMood, tvSuggestion;
    private ImageView ivJournalImage;
    private ImageButton btnBack, btnDelete;
    private JournalEntry entry;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_journal_detail);

        tvDate = findViewById(R.id.tvDate);
        tvTitle = findViewById(R.id.tvTitle);
        tvContent = findViewById(R.id.tvContent);
        tvMood = findViewById(R.id.tvMood);
        tvSuggestion = findViewById(R.id.tvSuggestion);
        ivJournalImage = findViewById(R.id.ivJournalImage);
        btnBack = findViewById(R.id.btnBack);
        btnDelete = findViewById(R.id.btnDelete);

        db = FirebaseFirestore.getInstance();

        if (getIntent().hasExtra("journal_entry")) {
            entry = (JournalEntry) getIntent().getSerializableExtra("journal_entry");
            displayEntry(entry);
        }

        btnBack.setOnClickListener(v -> finish());
        
        btnDelete.setOnClickListener(v -> deleteEntry());
    }

    private void displayEntry(JournalEntry entry) {
        if (entry == null) return;

        tvTitle.setText(entry.getTitle());
        tvContent.setText(entry.getContent());
        
        if (entry.getDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault());
            tvDate.setText(sdf.format(entry.getDate().toDate()));
        }

        if (entry.getImageUri() != null && !entry.getImageUri().isEmpty()) {
            ivJournalImage.setVisibility(View.VISIBLE);
            Glide.with(this).load(entry.getImageUri()).into(ivJournalImage);
        } else {
            ivJournalImage.setVisibility(View.GONE);
        }

        if (entry.getMood() != null && !entry.getMood().isEmpty()) {
            tvMood.setText(entry.getMood());
        } else {
            tvMood.setText("Not analyzed");
        }

        if (entry.getSuggestion() != null && !entry.getSuggestion().isEmpty()) {
            tvSuggestion.setText(entry.getSuggestion());
        } else {
            tvSuggestion.setText("No suggestion available");
        }
    }

    private void deleteEntry() {
        if (entry != null && entry.getId() != null) {
            db.collection("journals").document(entry.getId())
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(JournalDetailActivity.this, "Journal deleted", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(JournalDetailActivity.this, "Error deleting journal", Toast.LENGTH_SHORT).show();
                    });
        }
    }
}
