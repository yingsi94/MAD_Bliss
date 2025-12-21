package com.example.bliss;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GoalsActivity extends AppCompatActivity {

    // --- Views ---
    private Button btnMeditation, btnBreathing, btnGoals, btnAddGoal;
    private RecyclerView rvGoals;
    private ProgressBar progressBar, loadingProgress;
    private TextView tvProgressSubtitle, tvProgressPercentage;
    private LinearLayout emptyState;
    private LinearLayout chipContainer;

    // --- Data ---
    private GoalAdapter adapter;
    private List<Goal> goalList;
    private List<Goal> filteredGoalList;
    private Map<String, Button> categoryChipsMap;
    private Map<String, String> categoryEmojiMap;

    // --- State & Firebase ---
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private String userId;
    private String selectedCategory = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goals);

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Get current user ID
        if (auth.getCurrentUser() != null) {
            userId = auth.getCurrentUser().getUid();
        } else {
            // If user is not logged in, redirect to login
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        initializeViews();
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        selectTab(btnGoals);
        loadCategoriesAndSetupChips();
    }

    private void initializeViews() {
        btnMeditation = findViewById(R.id.btnMeditation);
        btnBreathing = findViewById(R.id.btnBreathing);
        btnGoals = findViewById(R.id.btnGoals);
        btnAddGoal = findViewById(R.id.btnAddGoal);

        chipContainer = findViewById(R.id.chipContainer);

        rvGoals = findViewById(R.id.rvGoals);
        progressBar = findViewById(R.id.progressBar);
        tvProgressSubtitle = findViewById(R.id.tvProgressSubtitle);
        tvProgressPercentage = findViewById(R.id.tvProgressPercentage);
        emptyState = findViewById(R.id.emptyState);
        loadingProgress = findViewById(R.id.loadingProgress);

        rvGoals.setLayoutManager(new LinearLayoutManager(this));
        goalList = new ArrayList<>();
        filteredGoalList = new ArrayList<>();
        adapter = new GoalAdapter(filteredGoalList);
        rvGoals.setAdapter(adapter);

        categoryChipsMap = new LinkedHashMap<>();
        categoryEmojiMap = new LinkedHashMap<>();
    }

    private void loadCategoriesAndSetupChips() {
        // Load categories from user's personal category collection
        firestore.collection("userCategories")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Map<String, String> categories = new LinkedHashMap<>();
                    categories.put("All", "");

                    // Add default categories
                    categories.put("Mindfulness", "🧘");
                    categories.put("Sleep", "😴");
                    categories.put("Journaling", "📔");
                    categories.put("Fitness", "💪");
                    categories.put("Gratitude", "🙏");

                    // If user has custom categories, add them
                    if (documentSnapshot.exists() && documentSnapshot.getData() != null) {
                        Map<String, Object> customCategories = documentSnapshot.getData();
                        for (Map.Entry<String, Object> entry : customCategories.entrySet()) {
                            String categoryName = entry.getKey();
                            String emoji = entry.getValue().toString();

                            // Don't override default categories
                            if (!categories.containsKey(categoryName)) {
                                categories.put(categoryName, emoji);
                            }
                        }
                    }

                    categoryEmojiMap.clear();
                    categoryEmojiMap.putAll(categories);

                    setupCategoryChips();
                    loadGoalsFromFirestore();
                })
                .addOnFailureListener(e -> {
                    // If loading fails, just use defaults
                    Map<String, String> categories = new LinkedHashMap<>();
                    categories.put("All", "");
                    categories.put("Mindfulness", "🧘");
                    categories.put("Sleep", "😴");
                    categories.put("Journaling", "📔");
                    categories.put("Fitness", "💪");
                    categories.put("Gratitude", "🙏");

                    categoryEmojiMap.clear();
                    categoryEmojiMap.putAll(categories);

                    setupCategoryChips();
                    loadGoalsFromFirestore();
                });
    }

    private void setupCategoryChips() {
        chipContainer.removeAllViews();
        categoryChipsMap.clear();

        for (Map.Entry<String, String> entry : categoryEmojiMap.entrySet()) {
            String category = entry.getKey();
            String emoji = entry.getValue();

            if (emoji == null || emoji.isEmpty()) {
                emoji = getDefaultEmoji(category);
            }

            MaterialButton chip = new MaterialButton(
                    this,
                    null,
                    com.google.android.material.R.attr.materialButtonOutlinedStyle
            );

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    getResources().getDimensionPixelSize(R.dimen.chip_height)
            );
            params.setMarginEnd(getResources().getDimensionPixelSize(R.dimen.chip_margin));
            chip.setLayoutParams(params);

            chip.setBackgroundTintList(null);

            chip.setText(emoji.isEmpty() ? category : emoji + " " + category);
            chip.setAllCaps(false);
            chip.setTextSize(12);
            chip.setOnClickListener(v -> filterGoals(category));

            chipContainer.addView(chip);
            categoryChipsMap.put(category, chip);
        }

        updateChipSelection(selectedCategory);
    }

    private void selectTab(Button selectedButton) {
        resetTab(btnMeditation);
        resetTab(btnBreathing);
        resetTab(btnGoals);
        selectedButton.setBackgroundResource(R.drawable.tab_selected_white);
        selectedButton.setTextColor(ContextCompat.getColor(this, android.R.color.white));
    }

    private void resetTab(Button button) {
        button.setBackgroundResource(android.R.color.transparent);
        button.setTextColor(ContextCompat.getColor(this, R.color.purple_dark));
    }

    private void setupListeners() {
        btnMeditation.setOnClickListener(v -> startActivity(new Intent(GoalsActivity.this, RelaxationActivity.class)));
        btnBreathing.setOnClickListener(v -> startActivity(new Intent(GoalsActivity.this, BreathingActivity.class)));
        btnGoals.setOnClickListener(v -> { /* Already on Goals tab */ });
        btnAddGoal.setOnClickListener(v -> startActivity(new Intent(GoalsActivity.this, AddEditGoalActivity.class)));
    }

    private void loadGoalsFromFirestore() {
        loadingProgress.setVisibility(View.VISIBLE);

        // Query only goals belonging to current user
        firestore.collection("goals")
                .whereEqualTo("userId", userId)
                .get()
                .addOnCompleteListener(task -> {
                    loadingProgress.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        goalList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Goal goal = document.toObject(Goal.class);
                            goal.setGoalId(document.getId());
                            if (goal.needsReset()) {
                                goal.setCurrentCount(0);
                                goal.setCompleted(false);
                                goal.setLastResetDate(Timestamp.now());
                                firestore.collection("goals").document(goal.getGoalId()).set(goal);
                            }
                            goalList.add(goal);
                        }
                        filterGoals(selectedCategory);
                        updateProgress();
                        updateEmptyState();
                    } else {
                        Toast.makeText(this, "Failed to load goals", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void filterGoals(String category) {
        selectedCategory = category;
        updateChipSelection(category);

        filteredGoalList.clear();
        if (category.equals("All")) {
            filteredGoalList.addAll(goalList);
        } else {
            for (Goal goal : goalList) {
                if (goal.getCategory().equals(category)) {
                    filteredGoalList.add(goal);
                }
            }
        }
        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateChipSelection(String category) {
        for (Button chip : categoryChipsMap.values()) {
            chip.setBackgroundResource(R.drawable.chip_unselected);
            chip.setTextColor(ContextCompat.getColor(this, R.color.purple_dark));
        }

        Button selectedChip = categoryChipsMap.get(category);
        if (selectedChip != null) {
            selectedChip.setBackgroundResource(R.drawable.chip_selected);
            selectedChip.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        }
    }

    private void updateProgress() {
        int completedCount = 0;
        for (Goal goal : goalList) {
            if (goal.isCompleted()) {
                completedCount++;
            }
        }
        int totalGoals = goalList.size();
        int percentage = totalGoals > 0 ? (completedCount * 100) / totalGoals : 0;
        progressBar.setProgress(percentage);
        tvProgressSubtitle.setText(completedCount + " of " + totalGoals + " goals completed");
        tvProgressPercentage.setText(percentage + "%");
    }

    private void updateEmptyState() {
        if (filteredGoalList.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            rvGoals.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            rvGoals.setVisibility(View.VISIBLE);
        }
    }

    private void updateGoalInFirestore(Goal goal) {
        firestore.collection("goals")
                .document(goal.getGoalId())
                .set(goal)
                .addOnSuccessListener(aVoid -> updateProgress())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to update goal", Toast.LENGTH_SHORT).show());
    }

    private void deleteGoal(Goal goal, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Goal")
                .setMessage("Are you sure you want to delete this goal?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    firestore.collection("goals")
                            .document(goal.getGoalId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                goalList.remove(goal);
                                filteredGoalList.remove(goal);
                                adapter.notifyDataSetChanged();
                                updateProgress();
                                updateEmptyState();
                                Toast.makeText(this, "Goal deleted", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Failed to delete goal", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String getDefaultEmoji(String category) {
        switch (category) {
            case "Mindfulness": return "🧘";
            case "Sleep": return "😴";
            case "Journaling": return "📔";
            case "Fitness": return "💪";
            case "Gratitude": return "🙏";
            default: return "";
        }
    }

    private String getCategoryEmoji(Goal goal) {
        if (goal.getCategoryEmoji() != null && !goal.getCategoryEmoji().isEmpty()) {
            return goal.getCategoryEmoji();
        }
        String defaultEmoji = getDefaultEmoji(goal.getCategory());
        if (!defaultEmoji.isEmpty()) {
            return defaultEmoji;
        }
        return "📌";
    }

    private class GoalAdapter extends RecyclerView.Adapter<GoalAdapter.ViewHolder> {
        private List<Goal> items;

        public GoalAdapter(List<Goal> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_goal, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Goal goal = items.get(position);

            holder.tvGoalText.setText(goal.getTitle());
            String categoryEmoji = getCategoryEmoji(goal);
            holder.tvCategory.setText(categoryEmoji + " " + goal.getCategory());

            holder.tvGoalType.setText(goal.getGoalType());
            holder.tvProgress.setText("Progress: " + goal.getProgressText());
            holder.tvProgressPercentage.setText(goal.getProgressPercentage() + "%");
            holder.progressBarGoal.setProgress(goal.getProgressPercentage());

            if (goal.getCurrentCount() >= goal.getTargetCount()) {
                goal.setCompleted(true);
                holder.btnMarkDone.setText("Completed");
                holder.btnMarkDone.setEnabled(false);
                holder.btnMarkDone.setBackgroundResource(R.drawable.button_completed);
            } else {
                goal.setCompleted(false);
                holder.btnMarkDone.setEnabled(true);
                holder.btnMarkDone.setBackgroundResource(R.drawable.button_mark_done);
                holder.btnMarkDone.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), android.R.color.white));
                if (goal.getTargetCount() > 1) {
                    holder.btnMarkDone.setText("Mark as Done (" + goal.getCurrentCount() + "/" + goal.getTargetCount() + ")");
                } else {
                    holder.btnMarkDone.setText("Mark as Done");
                }
            }

            holder.btnMarkDone.setOnClickListener(v -> {
                goal.setCurrentCount(goal.getCurrentCount() + 1);
                if (goal.getCurrentCount() >= goal.getTargetCount()) {
                    goal.setCompleted(true);
                }
                updateGoalInFirestore(goal);
                notifyItemChanged(holder.getAdapterPosition());
            });

            holder.btnOptions.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(v.getContext(), holder.btnOptions);
                popup.getMenuInflater().inflate(R.menu.goal_menu, popup.getMenu());
                popup.setOnMenuItemClickListener(item -> {
                    int itemId = item.getItemId();
                    if (itemId == R.id.action_edit) {
                        Intent intent = new Intent(GoalsActivity.this, AddEditGoalActivity.class);
                        intent.putExtra("goal_id", goal.getGoalId());
                        startActivity(intent);
                        return true;
                    } else if (itemId == R.id.action_delete) {
                        deleteGoal(goal, holder.getAdapterPosition());
                        return true;
                    }
                    return false;
                });
                popup.show();
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvGoalText, tvCategory, tvGoalType, tvProgress, tvProgressPercentage;
            ProgressBar progressBarGoal;
            Button btnMarkDone;
            ImageButton btnOptions;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvGoalText = itemView.findViewById(R.id.tvGoalText);
                tvCategory = itemView.findViewById(R.id.tvCategory);
                tvGoalType = itemView.findViewById(R.id.tvGoalType);
                tvProgress = itemView.findViewById(R.id.tvProgress);
                tvProgressPercentage = itemView.findViewById(R.id.tvProgressPercentage);
                progressBarGoal = itemView.findViewById(R.id.progressBarGoal);
                btnMarkDone = itemView.findViewById(R.id.btnMarkDone);
                btnOptions = itemView.findViewById(R.id.btnMenu);
            }
        }
    }
}