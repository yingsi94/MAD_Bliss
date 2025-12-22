package com.example.chatbox.models;

import java.io.Serializable;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Message implements Serializable {
    private String message;
    private String sender;
    private LocalTime createdAt;

    public Message(String message, String sender, LocalTime createdAt) {
        this.message = message;
        this.sender = sender;
        this.createdAt = createdAt;
    }

    public String getMessage() { return message; }
    public String getSender() { return sender; }

    // Formats the time to a clean "HH:mm" string for the UI
    public String getFormattedTime() {
        if (createdAt == null) return "";
        return createdAt.format(DateTimeFormatter.ofPattern("HH:mm"));
    }
}