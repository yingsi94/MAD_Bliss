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
        } else if (entry.getImageUri() != null && !entry.getImageUri().isEmpty()) {
            addImageToLayout(entry.getImageUri());
        }

        llJournalVideos.removeAllViews();
        if (entry.getVideoUris() != null && !entry.getVideoUris().isEmpty()) {
            for (int i = 0; i < entry.getVideoUris().size(); i++) {
                String uriStr = entry.getVideoUris().get(i);
                String thumb = (entry.getVideoThumbnails() != null && entry.getVideoThumbnails().size() > i) 
                        ? entry.getVideoThumbnails().get(i) : null;
                addVideoToLayout(Uri.parse(uriStr), thumb);
            }
        } else if (entry.getVideoUri() != null && !entry.getVideoUri().isEmpty()) {
            String thumb = (entry.getVideoThumbnails() != null && !entry.getVideoThumbnails().isEmpty()) 
                    ? entry.getVideoThumbnails().get(0) : null;
            addVideoToLayout(Uri.parse(entry.getVideoUri()), thumb);
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
            Intent intent = new Intent(this, FullScreenImageActivity.class);
            intent.putExtra("image_source", imageSource);
            startActivity(intent);
        });
        
        llJournalImages.addView(imageView);
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

        // On Click -> Play Video
        container.setOnClickListener(v -> {
            if (uri.toString().startsWith("http")) {
                // Remote URL (Cloudinary) - Play directly
                playVideo(uri);
            } else {
                // Local File - Check existence first
                try {
                    getContentResolver().openInputStream(uri).close();
                    playVideo(uri);
                } catch (Exception e) {
                    Toast.makeText(this, "Cannot play video: File not found on this device", Toast.LENGTH_LONG).show();
                }
            }
        });

        llJournalVideos.addView(container);
    }

    private void playVideo(Uri uri) {
        // Create a dialog or full screen activity to play video
        android.app.Dialog dialog = new android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.activity_full_screen_image); // Reuse layout but replace image with video
        
        android.widget.RelativeLayout root = dialog.findViewById(R.id.ivFullScreen).getParent() instanceof android.widget.RelativeLayout 
                ? (android.widget.RelativeLayout) dialog.findViewById(R.id.ivFullScreen).getParent() : null;
        
        if (root != null) {
            root.removeView(root.findViewById(R.id.ivFullScreen));
            
            VideoView videoView = new VideoView(this);
            android.widget.RelativeLayout.LayoutParams params = new android.widget.RelativeLayout.LayoutParams(
                    android.widget.RelativeLayout.LayoutParams.MATCH_PARENT,
                    android.widget.RelativeLayout.LayoutParams.MATCH_PARENT);
            params.addRule(android.widget.RelativeLayout.CENTER_IN_PARENT);
            videoView.setLayoutParams(params);
            
            MediaController mediaController = new MediaController(this);
            videoView.setMediaController(mediaController);
            mediaController.setAnchorView(videoView);
            
            root.addView(videoView, 0);
            
            // Add loading indicator
            android.widget.ProgressBar progressBar = new android.widget.ProgressBar(this);
            android.widget.RelativeLayout.LayoutParams progressParams = new android.widget.RelativeLayout.LayoutParams(
                    android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT);
            progressParams.addRule(android.widget.RelativeLayout.CENTER_IN_PARENT);
            progressBar.setLayoutParams(progressParams);
            root.addView(progressBar);

            videoView.setVideoURI(uri);
            android.util.Log.d("JournalDetail", "Attempting to play video: " + uri.toString());
            
            videoView.setOnPreparedListener(mp -> {
                progressBar.setVisibility(android.view.View.GONE);
                videoView.start();
            });
            
            videoView.setOnErrorListener((mp, what, extra) -> {
                progressBar.setVisibility(android.view.View.GONE);
                String errorMsg = "Unknown error";
                if (what == android.media.MediaPlayer.MEDIA_ERROR_SERVER_DIED) errorMsg = "Server died";
                else if (what == android.media.MediaPlayer.MEDIA_ERROR_UNKNOWN) errorMsg = "Unknown error";
                
                Toast.makeText(this, "Cannot play video: " + errorMsg + " (" + what + "," + extra + ")", Toast.LENGTH_LONG).show();
                return true;
            });
        }
        
        dialog.findViewById(R.id.btnClose).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // Old method kept for reference but unused
    private void addVideoToLayout(Uri uri) {
        addVideoToLayout(uri, null);
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
