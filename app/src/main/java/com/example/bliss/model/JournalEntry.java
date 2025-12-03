package com.example.bliss.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class JournalEntry implements Serializable {
    @Exclude private String id;
    private String title;
    private String content;
    private Date date; // Changed from Timestamp to Date for Serializable compatibility
    private String mood;
    private String suggestion;
    private String imageUri;
    private String videoUri;
    private List<String> imageUris;
    private List<String> videoUris;
    private List<String> videoThumbnails;

    public JournalEntry() {
        // Required empty constructor for Firestore
    }

    public JournalEntry(String title, String content, Timestamp date, String imageUri) {
        this.title = title;
        this.content = content;
        this.date = date != null ? date.toDate() : new Date();
        this.imageUri = imageUri;
    }

    @Exclude public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Timestamp getDate() { return date != null ? new Timestamp(date) : null; }
    public void setDate(Timestamp date) { this.date = date != null ? date.toDate() : null; }

    public String getMood() { return mood; }
    public void setMood(String mood) { this.mood = mood; }

    public String getSuggestion() { return suggestion; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }

    public String getImageUri() { return imageUri; }
    public void setImageUri(String imageUri) { this.imageUri = imageUri; }

    public String getVideoUri() { return videoUri; }
    public void setVideoUri(String videoUri) { this.videoUri = videoUri; }

    public List<String> getImageUris() { return imageUris; }
    public void setImageUris(List<String> imageUris) { this.imageUris = imageUris; }

    public List<String> getVideoUris() { return videoUris; }
    public void setVideoUris(List<String> videoUris) { this.videoUris = videoUris; }

    public List<String> getVideoThumbnails() { return videoThumbnails; }
    public void setVideoThumbnails(List<String> videoThumbnails) { this.videoThumbnails = videoThumbnails; }
}
