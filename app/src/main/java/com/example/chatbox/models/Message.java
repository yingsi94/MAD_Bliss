package com.example.chatbox.models;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Message implements Serializable {
    private String message;
    private String sender;
    private long createdAt; // Changed from LocalTime to long

    public Message(String message, String sender, long createdAt) {
        this.message = message;
        this.sender = sender;
        this.createdAt = createdAt;
    }

    public String getMessage() { return message; }
    public String getSender() { return sender; }

    // Formats the long timestamp to "HH:mm" (e.g., 21:15)
    public String getFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(new Date(createdAt));
    }
}