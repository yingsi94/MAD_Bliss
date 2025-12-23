package com.example.relaxation;

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
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bliss.R;
import com.example.login_signup_profile.LoginActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GoalsFragment extends Fragment {

    private Button btnMeditation, btnBreathing, btnGoals, btnAddGoal;
    private RecyclerView rvGoals;
    private ProgressBar progressBar, loadingProgress;
    private TextView tvProgressSubtitle, tvProgressPercentage;
    private LinearLayout emptyState, chipContainer;

    private GoalAdapter adapter;
    private List<Goal> goalList = new ArrayList<>();
    private List<Goal> filteredGoalList = new ArrayList<>();
    private Map<String, Button> categoryChipsMap = new LinkedHashMap<>();
    private Map<String, String> categoryEmojiMap = new LinkedHashMap<>();

    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private String userId;
    private String selectedCategory = "All";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_goals, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null) {
            userId = auth.getCurrentUser().getUid();
        } else {
            startActivity(new Intent(requireContext(), LoginActivity.class));
            requireActivity().finish();
            return;
        }

        initializeViews(view);
        setupListeners();
    }

    @Override
    public void onResume() {
        super.onResume();
        selectTab(btnGoals);
        loadCategoriesAndSetupChips();
    }

    private void initializeViews(View view) {
        btnMeditation = view.findViewById(R.id.btnMeditation);
        btnBreathing = view.findViewById(R.id.btnBreathing);
        btnGoals = view.findViewById(R.id.btnGoals);
        btnAddGoal = view.findViewById(R.id.btnAddGoal);
        chipContainer = view.findViewById(R.id.chipContainer);
        rvGoals = view.findViewById(R.id.rvGoals);
        progressBar = view.findViewById(R.id.progressBar);
        tvProgressSubtitle = view.findViewById(R.id.tvProgressSubtitle);
        tvProgressPercentage = view.findViewById(R.id.tvProgressPercentage);
        emptyState = view.findViewById(R.id.emptyState);
        loadingProgress = view.findViewById(R.id.loadingProgress);

        rvGoals.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new GoalAdapter(filteredGoalList);
        rvGoals.setAdapter(adapter);
    }

    private void setupListeners() {
        // Note: Replace these with Fragment navigation if using Navigation Component
        btnMeditation.setOnClickListener(v -> {
            // Logic to switch to Meditation Fragment
        });
        btnBreathing.setOnClickListener(v -> {
            // Logic to switch to Breathing Fragment
        });
        btnAddGoal.setOnClickListener(v -> startActivity(new Intent(requireContext(), AddEditGoalActivity.class)));
    }

    private void loadCategoriesAndSetupChips() {
        firestore.collection("userCategories").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    Map<String, String> categories = new LinkedHashMap<>();
                    categories.put("All", "");
                    categories.put("Mindfulness", "🧘");
                    categories.put("Sleep", "😴");
                    categories.put("Journaling", "📔");
                    categories.put("Fitness", "💪");
                    categories.put("Gratitude", "🙏");

                    if (documentSnapshot.exists() && documentSnapshot.getData() != null) {
                        for (Map.Entry<String, Object> entry : documentSnapshot.getData().entrySet()) {
                            if (!categories.containsKey(entry.getKey())) {
                                categories.put(entry.getKey(), entry.getValue().toString());
                            }
                        }
                    }
                    categoryEmojiMap.clear();
                    categoryEmojiMap.putAll(categories);
                    setupCategoryChips();
                    loadGoalsFromFirestore();
                })
                .addOnFailureListener(e -> loadGoalsFromFirestore());
    }

    private void setupCategoryChips() {
        chipContainer.removeAllViews();
        categoryChipsMap.clear();

        for (Map.Entry<String, String> entry : categoryEmojiMap.entrySet()) {
            String category = entry.getKey();
            String emoji = entry.getValue().isEmpty() ? getDefaultEmoji(category) : entry.getValue();

            MaterialButton chip = new MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, (int) getResources().getDimension(R.dimen.chip_height));
            params.setMarginEnd((int) getResources().getDimension(R.dimen.chip_margin));
            chip.setLayoutParams(params);
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
        selectedButton.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
    }

    private void resetTab(Button button) {
        button.setBackgroundResource(android.R.color.transparent);
        button.setTextColor(ContextCompat.getColor(requireContext(), R.color.purple_dark));
    }

    private void loadGoalsFromFirestore() {
        if (loadingProgress != null) loadingProgress.setVisibility(View.VISIBLE);
        firestore.collection("goals").whereEqualTo("userId", userId).get()
                .addOnCompleteListener(task -> {
                    if (loadingProgress != null) loadingProgress.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        goalList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Goal goal = document.toObject(Goal.class);
                            goal.setGoalId(document.getId());
                            goalList.add(goal);
                        }
                        filterGoals(selectedCategory);
                        updateProgress();
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
                if (goal.getCategory().equals(category)) filteredGoalList.add(goal);
            }
        }
        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateChipSelection(String category) {
        for (Button chip : categoryChipsMap.values()) {
            chip.setBackgroundResource(R.drawable.chip_unselected);
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.purple_dark));
        }
        Button selectedChip = categoryChipsMap.get(category);
        if (selectedChip != null) {
            selectedChip.setBackgroundResource(R.drawable.chip_selected);
            selectedChip.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
        }
    }

    private void updateProgress() {
        int completedCount = 0;
        for (Goal goal : goalList) if (goal.isCompleted()) completedCount++;
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

    // GoalAdapter remains largely the same but use requireContext() for UI
    private class GoalAdapter extends RecyclerView.Adapter<GoalAdapter.ViewHolder> {
        private List<Goal> items;
        public GoalAdapter(List<Goal> items) { this.items = items; }

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
            holder.tvCategory.setText(goal.getCategory());
            holder.progressBarGoal.setProgress(goal.getProgressPercentage());

            holder.btnMarkDone.setOnClickListener(v -> {
                goal.setCurrentCount(goal.getCurrentCount() + 1);
                firestore.collection("goals").document(goal.getGoalId()).set(goal);
                notifyItemChanged(position);
                updateProgress();
            });

            holder.btnOptions.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(requireContext(), v);
                popup.getMenuInflater().inflate(R.menu.goal_menu, popup.getMenu());
                popup.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == R.id.action_delete) {
                        firestore.collection("goals").document(goal.getGoalId()).delete();
                        goalList.remove(goal);
                        filterGoals(selectedCategory);
                        return true;
                    }
                    return false;
                });
                popup.show();
            });
        }

        @Override
        public int getItemCount() { return items.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvGoalText, tvCategory;
            ProgressBar progressBarGoal;
            Button btnMarkDone;
            ImageButton btnOptions;
            ViewHolder(View v) {
                super(v);
                tvGoalText = v.findViewById(R.id.tvGoalText);
                tvCategory = v.findViewById(R.id.tvCategory);
                progressBarGoal = v.findViewById(R.id.progressBarGoal);
                btnMarkDone = v.findViewById(R.id.btnMarkDone);
                btnOptions = v.findViewById(R.id.btnMenu);
            }
        }
    }
}