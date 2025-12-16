package com.example.bliss;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.example.bliss.model.JournalEntry;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class JournalDetailActivity extends AppCompatActivity {

    private TextView tvDate, tvTitle, tvContent, tvMood, tvSuggestion;
    private LinearLayout llJournalImages;
    private LinearLayout llJournalVideos;
    private ImageButton btnBack, btnDelete, btnEdit;
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
        llJournalImages = findViewById(R.id.llJournalImages);
        llJournalVideos = findViewById(R.id.llJournalVideos);
        btnBack = findViewById(R.id.btnBack);
        btnDelete = findViewById(R.id.btnDelete);
        btnEdit = findViewById(R.id.btnEdit);

        db = FirebaseFirestore.getInstance();

        if (getIntent().hasExtra("journal_entry")) {
            entry = (JournalEntry) getIntent().getSerializableExtra("journal_entry");
            displayEntry(entry);
        }

        btnBack.setOnClickListener(v -> finish());
        
        btnDelete.setOnClickListener(v -> deleteEntry());

        btnEdit.setOnClickListener(v -> {
            if (entry != null) {
                Intent intent = new Intent(JournalDetailActivity.this, AddJournalActivity.class);
                intent.putExtra("journal_entry", entry);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshEntry();
    }

    private void refreshEntry() {
        if (entry != null && entry.getId() != null) {
            db.collection("journals").document(entry.getId())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            JournalEntry updatedEntry = documentSnapshot.toObject(JournalEntry.class);
                            if (updatedEntry != null) {
                                updatedEntry.setId(documentSnapshot.getId());
                                entry = updatedEntry;
                                displayEntry(entry);
                            }
                        } else {
                            // Document might have been deleted
                            finish();
                        }
                    });
        }
    }

    private void displayEntry(JournalEntry entry) {
        if (entry == null) return;

        tvTitle.setText(entry.getTitle());
        tvContent.setText(entry.getContent());
        
        if (entry.getDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault());
            tvDate.setText(sdf.format(entry.getDate().toDate()));
        }

        llJournalImages.removeAllViews();
        if (entry.getImageUris() != null && !entry.getImageUris().isEmpty()) {
            for (String uriStr : entry.getImageUris()) {
                addImageToLayout(uriStr);
            }
        }

        llJournalVideos.removeAllViews();
        if (entry.getVideoUris() != null && !entry.getVideoUris().isEmpty()) {
            for (int i = 0; i < entry.getVideoUris().size(); i++) {
                String uriStr = entry.getVideoUris().get(i);
                // We don't have thumbnails anymore in the model, so passing null
                addVideoToLayout(Uri.parse(uriStr), null);
            }
        }

        if (entry.getMood() != null && !entry.getMood().isEmpty()) {
            String emoji = getMoodEmoji(entry.getMood());
            tvMood.setText(entry.getMood() + " " + emoji);
        } else {
            tvMood.setText("Not analyzed 😐");
        }

        if (entry.getSuggestion() != null && !entry.getSuggestion().isEmpty()) {
            tvSuggestion.setText(entry.getSuggestion());
        } else {
            tvSuggestion.setText("No suggestion available");
        }
    }

    private String getMoodEmoji(String mood) {
        if (mood == null) return "😐";
        String lowerMood = mood.toLowerCase();
        if (lowerMood.contains("happy") || lowerMood.contains("joy") || lowerMood.contains("excited")) {
            return "😊";
        } else if (lowerMood.contains("sad") || lowerMood.contains("depressed") || lowerMood.contains("down")) {
            return "😢";
        } else if (lowerMood.contains("angry") || lowerMood.contains("mad") || lowerMood.contains("frustrated")) {
            return "😠";
        } else if (lowerMood.contains("anxious") || lowerMood.contains("nervous") || lowerMood.contains("worried")) {
            return "😰";
        } else if (lowerMood.contains("calm") || lowerMood.contains("peaceful") || lowerMood.contains("relaxed")) {
            return "😌";
        } else if (lowerMood.contains("tired") || lowerMood.contains("exhausted")) {
            return "😴";
        } else if (lowerMood.contains("love") || lowerMood.contains("grateful")) {
            return "🥰";
        } else {
            return "😐";
        }
    }

    private void addImageToLayout(String imageSource) {
        ImageView imageView = new ImageView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(400, 400);
        params.setMargins(8, 0, 8, 0);
        imageView.setLayoutParams(params);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        
        // Handle different image source types
        if (imageSource.startsWith("http")) {
            // Cloudinary or other web URL
            Glide.with(this)
                .load(imageSource)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_gallery)
                .into(imageView);
        } else if (imageSource.startsWith("content") || imageSource.startsWith("file")) {
            // Local URI
            Glide.with(this).load(imageSource).into(imageView);
        } else {
            // Base64 (fallback for old entries)
            try {
                byte[] imageBytes = Base64.decode(imageSource, Base64.DEFAULT);
                Glide.with(this)
                    .asBitmap()
                    .load(imageBytes)
                    .into(imageView);
            } catch (IllegalArgumentException e) {
                Glide.with(this)
                    .load(imageSource)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .into(imageView);
            }
        }
        
        // Click to open full screen
        imageView.setOnClickListener(v -> {
            openFullScreenMedia(imageSource);
        });
        
        llJournalImages.addView(imageView);
    }

    private void openFullScreenMedia(String currentMediaUri) {
        Intent intent = new Intent(this, FullScreenImageActivity.class);
        
        java.util.ArrayList<String> mediaUris = new java.util.ArrayList<>();
        java.util.ArrayList<String> mediaTypes = new java.util.ArrayList<>();
        
        // Add all images
        if (entry.getImageUris() != null) {
            for (String uri : entry.getImageUris()) {
                mediaUris.add(uri);
                mediaTypes.add("image");
            }
        }
        
        // Add all videos
        if (entry.getVideoUris() != null) {
            for (String uri : entry.getVideoUris()) {
                mediaUris.add(uri);
                mediaTypes.add("video");
            }
        }
        
        // Find index
        int index = mediaUris.indexOf(currentMediaUri);
        if (index == -1) index = 0;
        
        intent.putStringArrayListExtra("media_uris", mediaUris);
        intent.putStringArrayListExtra("media_types", mediaTypes);
        intent.putExtra("start_index", index);
        
        startActivity(intent);
    }

    private void addVideoToLayout(Uri uri, String thumbnailBase64) {
        // Use a FrameLayout to show thumbnail first, then video on click
        android.widget.FrameLayout container = new android.widget.FrameLayout(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(400, 400);
        params.setMargins(8, 0, 8, 0);
        container.setLayoutParams(params);

        // Thumbnail View
        ImageView thumbView = new ImageView(this);
        android.widget.FrameLayout.LayoutParams matchParent = new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT);
        thumbView.setLayoutParams(matchParent);
        thumbView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        
        // Load Thumbnail
        if (thumbnailBase64 != null && !thumbnailBase64.isEmpty()) {
            // Base64 Thumbnail (Old local videos)
            try {
                byte[] imageBytes = Base64.decode(thumbnailBase64, Base64.DEFAULT);
                Glide.with(this).asBitmap().load(imageBytes).into(thumbView);
            } catch (Exception e) {
                thumbView.setImageResource(android.R.drawable.ic_media_play);
            }
        } else {
            // No Base64 thumbnail -> Try loading from Video URL (Cloudinary or Local)
            Glide.with(this)
                .asBitmap()
                .load(uri) // Glide can generate thumbnails from video URLs!
                .placeholder(android.R.drawable.ic_media_play)
                .error(android.R.drawable.ic_media_play)
                .into(thumbView);
        }

        // Play Icon Overlay
        ImageView playIcon = new ImageView(this);
        android.widget.FrameLayout.LayoutParams centerParams = new android.widget.FrameLayout.LayoutParams(100, 100);
        centerParams.gravity = android.view.Gravity.CENTER;
        playIcon.setLayoutParams(centerParams);
        playIcon.setImageResource(android.R.drawable.ic_media_play);
        playIcon.setColorFilter(android.graphics.Color.WHITE);
        playIcon.setBackgroundResource(R.drawable.circle_background);
        playIcon.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#80000000")));

        container.addView(thumbView);
        container.addView(playIcon);

        // On Click -> Open Full Screen Media Viewer
        container.setOnClickListener(v -> {
            openFullScreenMedia(uri.toString());
        });

        llJournalVideos.addView(container);
    }

    private void deleteEntry() {
        if (entry == null || entry.getId() == null) return;

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Entry")
                .setMessage("Are you sure you want to delete this journal entry?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.collection("journals").document(entry.getId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(JournalDetailActivity.this, "Entry deleted", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> Toast.makeText(JournalDetailActivity.this, "Error deleting entry", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
