package com.example.bliss;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.example.bliss.ai.GeminiHelper;
import com.example.bliss.model.JournalEntry;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class AddJournalActivity extends AppCompatActivity {

    private EditText etTitle, etContent;
    private Button btnSave, btnCancel, btnAnalyze, btnAddPhoto, btnAddVideo;
    private ImageButton btnBack;
    private LinearLayout llSelectedImages;
    private LinearLayout llSelectedVideos;
    private LinearLayout cvAiSuggestion;
    private TextView tvAiSuggestion;
    private android.widget.ScrollView svMain;
    // Progress UI
    private LinearLayout llProgress;
    private android.widget.ProgressBar progressBar;
    private TextView tvProgressStatus;
    
    private FirebaseFirestore db;
    private GeminiHelper geminiHelper;
    private CloudinaryHelper cloudinaryHelper;
    private String currentMood = "";
    private String currentSuggestion = "";
    private String entryId = null;
    private List<Uri> selectedImageUris = new ArrayList<>();
    private List<Uri> selectedVideoUris = new ArrayList<>();
    
    private ActivityResultLauncher<PickVisualMediaRequest> pickMultipleMedia;

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
        btnAddPhoto = findViewById(R.id.btnAddPhoto);
        btnAddVideo = findViewById(R.id.btnAddVideo);
        llSelectedImages = findViewById(R.id.llSelectedImages);
        llSelectedVideos = findViewById(R.id.llSelectedVideos);
        
        llProgress = findViewById(R.id.llProgress);
        progressBar = findViewById(R.id.progressBar);
        tvProgressStatus = findViewById(R.id.tvProgressStatus);

        db = FirebaseFirestore.getInstance();
        geminiHelper = new GeminiHelper();
        cloudinaryHelper = new CloudinaryHelper(this);

        pickMultipleMedia = registerForActivityResult(new ActivityResultContracts.PickMultipleVisualMedia(5), uris -> {
            if (uris != null && !uris.isEmpty()) {
                String mimeType = getContentResolver().getType(uris.get(0));
                if (mimeType != null && mimeType.startsWith("video")) {
                    selectedVideoUris.clear();
                    selectedVideoUris.addAll(uris);
                    showVideos();
                } else {
                    selectedImageUris.clear();
                    selectedImageUris.addAll(uris);
                    showImages();
                }
            }
        });

        btnAddPhoto.setOnClickListener(v -> pickMultipleMedia.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build()));

        btnAddVideo.setOnClickListener(v -> pickMultipleMedia.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.VideoOnly.INSTANCE)
                .build()));

        // Check for existing entry to edit
        if (getIntent().hasExtra("journal_entry")) {
            JournalEntry entry = (JournalEntry) getIntent().getSerializableExtra("journal_entry");
            if (entry != null) {
                entryId = entry.getId();
                etTitle.setText(entry.getTitle());
                etContent.setText(entry.getContent());
                currentMood = entry.getMood();
                currentSuggestion = entry.getSuggestion();
                
                if (currentSuggestion != null && !currentSuggestion.isEmpty()) {
                    cvAiSuggestion.setVisibility(View.VISIBLE);
                    tvAiSuggestion.setText(currentSuggestion);
                }
                
                // Load existing images
                if (entry.getImageUris() != null && !entry.getImageUris().isEmpty()) {
                    for (String uriStr : entry.getImageUris()) {
                        selectedImageUris.add(Uri.parse(uriStr));
                    }
                    showImages();
                } else if (entry.getImageUri() != null) {
                    selectedImageUris.add(Uri.parse(entry.getImageUri()));
                    showImages();
                }

                // Load existing videos
                if (entry.getVideoUris() != null && !entry.getVideoUris().isEmpty()) {
                    for (String uriStr : entry.getVideoUris()) {
                        selectedVideoUris.add(Uri.parse(uriStr));
                    }
                    showVideos();
                } else if (entry.getVideoUri() != null) {
                    selectedVideoUris.add(Uri.parse(entry.getVideoUri()));
                    showVideos();
                }
            }
        }

        btnBack.setOnClickListener(v -> finish());
        btnCancel.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveEntry());

        btnAnalyze.setOnClickListener(v -> {
            String title = etTitle.getText().toString();
            String content = etContent.getText().toString();
            
            if (content.isEmpty()) {
                Toast.makeText(this, "Please write something first", Toast.LENGTH_SHORT).show();
                return;
            }

            btnAnalyze.setEnabled(false);
            btnAnalyze.setText("Analyzing...");
            
            geminiHelper.analyzeJournalEntry(title, content, new GeminiHelper.AnalysisCallback() {
                @Override
                public void onAnalysisSuccess(String result) {
                    runOnUiThread(() -> {
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
        });
    }

    private void showImages() {
        llSelectedImages.removeAllViews();
        for (Uri uri : selectedImageUris) {
            // Container
            FrameLayout container = new FrameLayout(this);
            LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(400, 400);
            containerParams.setMargins(8, 8, 8, 8);
            container.setLayoutParams(containerParams);

            // Image
            ImageView imageView = new ImageView(this);
            FrameLayout.LayoutParams imgParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, 
                    FrameLayout.LayoutParams.MATCH_PARENT);
            imageView.setLayoutParams(imgParams);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Glide.with(this).load(uri).into(imageView);
            
            // Click to open full screen
            imageView.setOnClickListener(v -> {
                Intent intent = new Intent(this, FullScreenImageActivity.class);
                intent.putExtra("image_source", uri.toString());
                startActivity(intent);
            });

            // Delete Button (X)
            ImageButton btnDelete = new ImageButton(this);
            int btnSize = 70; // Increased size
            FrameLayout.LayoutParams btnParams = new FrameLayout.LayoutParams(btnSize, btnSize);
            btnParams.gravity = Gravity.TOP | Gravity.END;
            btnParams.setMargins(0, 0, 0, 0); // Remove margins to be right at the corner
            btnDelete.setLayoutParams(btnParams);
            btnDelete.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            btnDelete.setBackgroundResource(R.drawable.circle_background); 
            btnDelete.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.RED)); // Red background
            btnDelete.setColorFilter(Color.WHITE);
            btnDelete.setPadding(12, 12, 12, 12);
            btnDelete.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

            btnDelete.setOnClickListener(v -> {
                selectedImageUris.remove(uri);
                showImages();
                Toast.makeText(this, "Image removed", Toast.LENGTH_SHORT).show();
            });

            container.addView(imageView);
            container.addView(btnDelete);
            llSelectedImages.addView(container);
        }
    }

    private void showVideos() {
        llSelectedVideos.removeAllViews();
        for (Uri uri : selectedVideoUris) {
            // Container
            FrameLayout container = new FrameLayout(this);
            LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(400, 400);
            containerParams.setMargins(8, 8, 8, 8);
            container.setLayoutParams(containerParams);

            // Thumbnail
            ImageView imageView = new ImageView(this);
            FrameLayout.LayoutParams imgParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, 
                    FrameLayout.LayoutParams.MATCH_PARENT);
            imageView.setLayoutParams(imgParams);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Glide.with(this).load(uri).into(imageView);
            
            // Delete Button (X)
            ImageButton btnDelete = new ImageButton(this);
            int btnSize = 70; // Increased size
            FrameLayout.LayoutParams btnParams = new FrameLayout.LayoutParams(btnSize, btnSize);
            btnParams.gravity = Gravity.TOP | Gravity.END;
            btnParams.setMargins(0, 0, 0, 0);
            btnDelete.setLayoutParams(btnParams);
            btnDelete.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            btnDelete.setBackgroundResource(R.drawable.circle_background);
            btnDelete.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.RED)); // Red background
            btnDelete.setColorFilter(Color.WHITE);
            btnDelete.setPadding(12, 12, 12, 12);
            btnDelete.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

            btnDelete.setOnClickListener(v -> {
                selectedVideoUris.remove(uri);
                showVideos();
                Toast.makeText(this, "Video removed", Toast.LENGTH_SHORT).show();
            });

            container.addView(imageView);
            container.addView(btnDelete);
            llSelectedVideos.addView(container);
        }
    }

    private void saveEntry() {
        String title = etTitle.getText().toString();
        String content = etContent.getText().toString();

        if (title.isEmpty() || content.isEmpty()) {
            Toast.makeText(this, "Please fill in title and content", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);
        Toast.makeText(this, "Uploading media...", Toast.LENGTH_SHORT).show();
        
        uploadMediaToCloudinary(title, content);
    }

    private void uploadMediaToCloudinary(String title, String content) {
        List<String> uploadedImageUrls = new ArrayList<>();
        List<String> uploadedVideoUrls = new ArrayList<>();
        
        int totalUploads = selectedImageUris.size() + selectedVideoUris.size();
        
        if (totalUploads == 0) {
            // No media to upload, save directly
            saveToFirestore(title, content, uploadedImageUrls, uploadedVideoUrls, new ArrayList<>());
            return;
        }
        
        // Show Progress
        llProgress.setVisibility(View.VISIBLE);
        progressBar.setMax(totalUploads * 100);
        progressBar.setProgress(0);
        
        final int[] completedUploads = {0};
        final boolean[] hasError = {false};
        final int[] currentProgress = {0}; // Track total progress
        
        // Upload Images
        for (Uri imageUri : selectedImageUris) {
            cloudinaryHelper.uploadImage(imageUri, new CloudinaryHelper.UploadListener() {
                @Override
                public void onUploadSuccess(String publicUrl) {
                    runOnUiThread(() -> {
                        if (hasError[0]) return;
                        
                        uploadedImageUrls.add(publicUrl);
                        completedUploads[0]++;
                        
                        // Update status
                        tvProgressStatus.setText("Uploaded " + completedUploads[0] + "/" + totalUploads + " items");
                        
                        if (completedUploads[0] == totalUploads) {
                            tvProgressStatus.setText("Saving journal...");
                            saveToFirestore(title, content, uploadedImageUrls, uploadedVideoUrls, new ArrayList<>());
                        }
                    });
                }
                
                @Override
                public void onUploadFailure(String error) {
                    runOnUiThread(() -> {
                        if (hasError[0]) return;
                        hasError[0] = true;
                        llProgress.setVisibility(View.GONE);
                        Toast.makeText(AddJournalActivity.this, 
                            "Upload failed: " + error, Toast.LENGTH_LONG).show();
                        btnSave.setEnabled(true);
                    });
                }
                
                @Override
                public void onUploadProgress(int progress) {
                    // Simple progress update (just indeterminate or per-item is hard to aggregate perfectly without complex logic)
                    // For now, we just rely on item count for the bar, or we could try to sum it up.
                    // Let's just update the bar based on completed items for simplicity and reliability
                    runOnUiThread(() -> {
                         int totalProgress = (completedUploads[0] * 100) + progress;
                         progressBar.setProgress(totalProgress);
                    });
                }
            });
        }
        
        // Upload Videos
        for (Uri videoUri : selectedVideoUris) {
            cloudinaryHelper.uploadVideo(videoUri, new CloudinaryHelper.UploadListener() {
                @Override
                public void onUploadSuccess(String publicUrl) {
                    runOnUiThread(() -> {
                        if (hasError[0]) return;
                        
                        uploadedVideoUrls.add(publicUrl);
                        completedUploads[0]++;
                        
                        tvProgressStatus.setText("Uploaded " + completedUploads[0] + "/" + totalUploads + " items");
                        
                        if (completedUploads[0] == totalUploads) {
                            tvProgressStatus.setText("Saving journal...");
                            saveToFirestore(title, content, uploadedImageUrls, uploadedVideoUrls, new ArrayList<>());
                        }
                    });
                }
                
                @Override
                public void onUploadFailure(String error) {
                    runOnUiThread(() -> {
                        if (hasError[0]) return;
                        hasError[0] = true;
                        llProgress.setVisibility(View.GONE);
                        Toast.makeText(AddJournalActivity.this, 
                            "Video upload failed: " + error, Toast.LENGTH_LONG).show();
                        btnSave.setEnabled(true);
                    });
                }
                
                @Override
                public void onUploadProgress(int progress) {
                    runOnUiThread(() -> {
                         int totalProgress = (completedUploads[0] * 100) + progress;
                         progressBar.setProgress(totalProgress);
                    });
                }
            });
        }
    }

    private void saveToFirestore(String title, String content, List<String> imageUrls, List<String> videoUrls, List<String> videoThumbnails) {
        JournalEntry entry = new JournalEntry();
        entry.setTitle(title);
        entry.setContent(content);
        entry.setDate(new Timestamp(new Date()));
        entry.setImageUris(imageUrls);
        entry.setVideoUris(videoUrls);
        entry.setVideoThumbnails(videoThumbnails);
        entry.setMood(currentMood);
        entry.setSuggestion(currentSuggestion);
        
        if (!imageUrls.isEmpty()) entry.setImageUri(imageUrls.get(0));
        if (!videoUrls.isEmpty()) entry.setVideoUri(videoUrls.get(0));

        if (entryId != null) {
            entry.setId(entryId);
            db.collection("journals").document(entryId)
                    .set(entry)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(AddJournalActivity.this, "Journal updated!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(AddJournalActivity.this, "Error updating journal: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        btnSave.setEnabled(true);
                    });
        } else {
            db.collection("journals")
                    .add(entry)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(AddJournalActivity.this, "Journal saved!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(AddJournalActivity.this, "Error saving journal: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        btnSave.setEnabled(true);
                    });
        }
    }
}
