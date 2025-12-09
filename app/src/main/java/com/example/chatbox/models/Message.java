package com.example.chatbox.models;

import java.time.LocalTime;

public class Message {
    String message;
    String sender;
    LocalTime createdAt;

    // Constructor
    public Message(String message, String sender, LocalTime createdAt) {
        this.message = message;
        this.sender = sender;
        this.createdAt = createdAt;
    }

    // Getters
    public String getMessage() {
        return message;
    }

    public String getSender() {
        return sender;
    }

    public LocalTime getCreatedAt() {
        return createdAt;
    }
}
