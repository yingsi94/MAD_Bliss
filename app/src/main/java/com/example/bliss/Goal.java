package com.example.bliss;

import com.google.firebase.Timestamp;
import java.util.Calendar;
import java.util.TimeZone;

public class Goal {
    private String goalId;
    private String title;
    private String category;
    private String categoryEmoji; // Stores custom emoji for non-standard categories
    private String goalType; // "Daily" or "Weekly"
    private int targetCount;
    private int currentCount;
    private Timestamp lastResetDate;
    private boolean isCompleted;
    private Timestamp createdAt;

    // Empty constructor is required for Firestore's automatic data mapping
    public Goal() {}

    // You can keep a full constructor for testing or other purposes
    public Goal(String goalId, String title, String category, String categoryEmoji,
                String goalType, int targetCount, int currentCount,
                Timestamp lastResetDate, boolean isCompleted, Timestamp createdAt) {
        this.goalId = goalId;
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

    // --- Getters and Setters for all fields ---

    public String getGoalId() { return goalId; }
    public void setGoalId(String goalId) { this.goalId = goalId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    // Getter and Setter for the custom emoji
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


    // --- Helper Methods ---

    /**
     * Calculates the completion progress as a percentage.
     */
    public int getProgressPercentage() {
        if (targetCount <= 0) {
            return 0;
        }
        // Ensure progress doesn't exceed 100%
        int progress = (int) (((double) currentCount / targetCount) * 100);
        return Math.min(progress, 100);
    }

    /**
     * Returns the progress as a formatted string (e.g., "1 / 3").
     */
    public String getProgressText() {
        return currentCount + " / " + targetCount;
    }

    /**
     * Checks if the goal needs to be reset based on its type (Daily/Weekly)
     * and the last reset date. Resets happen at 12:00 AM.
     */
    public boolean needsReset() {
        if (lastResetDate == null || goalType == null) {
            // If it's a new goal (lastResetDate is null), don't reset yet.
            return false;
        }

        // 1. Define the Time Zone (Use the device's local time zone)
        TimeZone deviceTimeZone = TimeZone.getDefault();

        // 2. Calculate Today's Midnight (The current reset boundary)
        Calendar now = Calendar.getInstance(deviceTimeZone);
        now.set(Calendar.HOUR_OF_DAY, 0);
        now.set(Calendar.MINUTE, 0);
        now.set(Calendar.SECOND, 0);
        now.set(Calendar.MILLISECOND, 0);

        // 3. Calculate the Last Reset Boundary
        Calendar lastResetBoundary = Calendar.getInstance(deviceTimeZone);
        lastResetBoundary.setTime(lastResetDate.toDate()); // Convert Firestore Timestamp (UTC) to Date (Local)

        // Set last reset to its midnight (for accurate date comparison)
        lastResetBoundary.set(Calendar.HOUR_OF_DAY, 0);
        lastResetBoundary.set(Calendar.MINUTE, 0);
        lastResetBoundary.set(Calendar.SECOND, 0);
        lastResetBoundary.set(Calendar.MILLISECOND, 0);

        if (goalType.equals("Daily")) {
            // Daily goal: Reset if today's midnight is strictly AFTER the last reset's midnight.
            // We use isAfter() semantics.
            return now.after(lastResetBoundary);

        } else if (goalType.equals("Weekly")) {
            // Weekly goal: The boundary is 7 days AFTER the last reset date.

            // Find the next required reset date by adding 7 days to the last reset date's midnight
            Calendar nextReset = (Calendar) lastResetBoundary.clone();
            nextReset.add(Calendar.DAY_OF_YEAR, 7);

            // Reset if today's midnight is ON or AFTER the next required reset date.
            return !now.before(nextReset);
        }

        return false;
    }

}
