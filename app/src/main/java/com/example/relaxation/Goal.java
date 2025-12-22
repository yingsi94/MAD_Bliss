package com.example.relaxation;

import com.google.firebase.Timestamp;
import java.util.Calendar;
import java.util.TimeZone;

public class Goal {
    private String goalId;
    private String userId; // NEW FIELD - to store which user owns this goal
    private String title;
    private String category;
    private String categoryEmoji;
    private String goalType;
    private int targetCount;
    private int currentCount;
    private Timestamp lastResetDate;
    private boolean isCompleted;
    private Timestamp createdAt;

    // Empty constructor required for Firestore
    public Goal() {}

    // Full constructor with userId
    public Goal(String goalId, String userId, String title, String category, String categoryEmoji,
                String goalType, int targetCount, int currentCount,
                Timestamp lastResetDate, boolean isCompleted, Timestamp createdAt) {
        this.goalId = goalId;
        this.userId = userId;
        this.title = title;
        this.category = category;
        this.categoryEmoji = categoryEmoji;
        this.goalType = goalType;
        this.targetCount = targetCount;
        this.currentCount = currentCount;
        this.lastResetDate = lastResetDate;
        this.isCompleted = isCompleted;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public String getGoalId() { return goalId; }
    public void setGoalId(String goalId) { this.goalId = goalId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getCategoryEmoji() { return categoryEmoji; }
    public void setCategoryEmoji(String categoryEmoji) { this.categoryEmoji = categoryEmoji; }

    public String getGoalType() { return goalType; }
    public void setGoalType(String goalType) { this.goalType = goalType; }

    public int getTargetCount() { return targetCount; }
    public void setTargetCount(int targetCount) { this.targetCount = targetCount; }

    public int getCurrentCount() { return currentCount; }
    public void setCurrentCount(int currentCount) { this.currentCount = currentCount; }

    public Timestamp getLastResetDate() { return lastResetDate; }
    public void setLastResetDate(Timestamp lastResetDate) { this.lastResetDate = lastResetDate; }

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    // Helper Methods
    public int getProgressPercentage() {
        if (targetCount <= 0) {
            return 0;
        }
        int progress = (int) (((double) currentCount / targetCount) * 100);
        return Math.min(progress, 100);
    }

    public String getProgressText() {
        return currentCount + " / " + targetCount;
    }

    public boolean needsReset() {
        if (lastResetDate == null || goalType == null) {
            return false;
        }

        TimeZone deviceTimeZone = TimeZone.getDefault();

        Calendar now = Calendar.getInstance(deviceTimeZone);
        now.set(Calendar.HOUR_OF_DAY, 0);
        now.set(Calendar.MINUTE, 0);
        now.set(Calendar.SECOND, 0);
        now.set(Calendar.MILLISECOND, 0);

        Calendar lastResetBoundary = Calendar.getInstance(deviceTimeZone);
        lastResetBoundary.setTime(lastResetDate.toDate());

        lastResetBoundary.set(Calendar.HOUR_OF_DAY, 0);
        lastResetBoundary.set(Calendar.MINUTE, 0);
        lastResetBoundary.set(Calendar.SECOND, 0);
        lastResetBoundary.set(Calendar.MILLISECOND, 0);

        if (goalType.equals("Daily")) {
            return now.after(lastResetBoundary);
        } else if (goalType.equals("Weekly")) {
            Calendar nextReset = (Calendar) lastResetBoundary.clone();
            nextReset.add(Calendar.DAY_OF_YEAR, 7);
            return !now.before(nextReset);
        }

        return false;
    }
}