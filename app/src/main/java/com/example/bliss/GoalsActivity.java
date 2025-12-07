package com.example.bliss;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;

public class GoalsActivity extends AppCompatActivity {

    private Button btnMeditation, btnBreathing, btnGoals, btnAddGoal;
    private RecyclerView rvGoals;
    private GoalAdapter adapter;
    private List<GoalItem> goalList;
    private ProgressBar progressBar;
    private TextView tvProgressSubtitle, tvProgressPercentage;
    private BottomNavigationView bottomNav;
    private FloatingActionButton fabChat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goals);

        // Initialize views
        btnMeditation = findViewById(R.id.btnMeditation);
        btnBreathing = findViewById(R.id.btnBreathing);
        btnGoals = findViewById(R.id.btnGoals);
        btnAddGoal = findViewById(R.id.btnAddGoal);
        rvGoals = findViewById(R.id.rvGoals);
        progressBar = findViewById(R.id.progressBar);
        tvProgressSubtitle = findViewById(R.id.tvProgressSubtitle);
        tvProgressPercentage = findViewById(R.id.tvProgressPercentage);

        // Initialize RecyclerView
        rvGoals.setLayoutManager(new LinearLayoutManager(this));

        // Create sample goals
        goalList = new ArrayList<>();
        goalList.add(new GoalItem("Meditate for 10 minutes daily", true));
        goalList.add(new GoalItem("Sleep 8 hours nightly", true));
        goalList.add(new GoalItem("Journal 3 times this week", false));
        goalList.add(new GoalItem("Exercise 30 minutes", false));
        goalList.add(new GoalItem("Journal 3 times this week", false));

        // Set adapter
        adapter = new GoalAdapter(goalList);
        rvGoals.setAdapter(adapter);

        // Update progress
        updateProgress();

        // Tab button listeners
        btnMeditation.setOnClickListener(v -> {
            finish(); // Go back to RelaxationActivity (Meditation tab)
        });

        btnBreathing.setOnClickListener(v -> {
            Intent intent = new Intent(GoalsActivity.this, BreathingActivity.class);
            startActivity(intent);
            finish();
        });

        btnGoals.setOnClickListener(v -> {
            // Already on Goals tab
        });

        // Add goal button
        btnAddGoal.setOnClickListener(v -> {
            Toast.makeText(this, "Add goal dialog coming soon", Toast.LENGTH_SHORT).show();
            // You can implement a dialog or new activity to add goals
        });
    }
    private void updateProgress() {
        int completedCount = 0;
        for (GoalItem goal : goalList) {
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

    // Goal Item Model Class
    public static class GoalItem {
        private String text;
        private boolean completed;

        public GoalItem(String text, boolean completed) {
            this.text = text;
            this.completed = completed;
        }

        public String getText() {
            return text;
        }

        public boolean isCompleted() {
            return completed;
        }

        public void setCompleted(boolean completed) {
            this.completed = completed;
        }
    }

    // RecyclerView Adapter
    public class GoalAdapter extends RecyclerView.Adapter<GoalAdapter.ViewHolder> {
        private List<GoalItem> items;

        public GoalAdapter(List<GoalItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_goal, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            GoalItem item = items.get(position);
            holder.tvGoalText.setText(item.getText());
            holder.cbGoal.setChecked(item.isCompleted());

            // Checkbox click listener
            holder.cbGoal.setOnCheckedChangeListener((buttonView, isChecked) -> {
                item.setCompleted(isChecked);
                updateProgress();
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            CheckBox cbGoal;
            TextView tvGoalText;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                cbGoal = itemView.findViewById(R.id.cbGoal);
                tvGoalText = itemView.findViewById(R.id.tvGoalText);
            }
        }
    }
}