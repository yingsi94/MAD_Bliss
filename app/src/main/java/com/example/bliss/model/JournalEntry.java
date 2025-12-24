package com.example.bliss.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class JournalEntry implements Serializable {
    @Exclude private String id;
    private String userId;
    private String title;
    private String content;
    private Date date; // Changed from Timestamp to Date for Serializable compatibility
    private String mood;
    private String suggestion;
    private List<String> imageUris;
    private List<String> videoUris;

    public JournalEntry() {
        // Required empty constructor for Firestore
    }

    public JournalEntry(String title, String content, Timestamp date) {
        this.title = title;
        this.content = content;
        this.date = date != null ? date.toDate() : new Date();
    }

    @Exclude public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

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

    public List<String> getImageUris() { return imageUris; }
    public void setImageUris(List<String> imageUris) { this.imageUris = imageUris; }

    public List<String> getVideoUris() { return videoUris; }
    public void setVideoUris(List<String> videoUris) { this.videoUris = videoUris; }
}
