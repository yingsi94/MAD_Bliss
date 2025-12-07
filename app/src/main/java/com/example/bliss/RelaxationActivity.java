package com.example.bliss;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class RelaxationActivity extends AppCompatActivity {
    private RecyclerView rvMeditations;
    private MeditationAdapter adapter;
    private List<MeditationItem> meditationList;
    private Button btnMeditation, btnBreathing, btnGoals;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_relaxation);

        // Initialize RecyclerView
        rvMeditations = findViewById(R.id.rvMeditations);
        rvMeditations.setLayoutManager(new LinearLayoutManager(this));

        //Initialize tab buttons
        btnMeditation = findViewById(R.id.btnMeditation);
        btnBreathing = findViewById(R.id.btnBreathing);
        btnGoals = findViewById(R.id.btnGoals);

        // Create meditation items
        meditationList = new ArrayList<>();
        meditationList.add(new MeditationItem("Body Scan", "Progressive relaxation", "10 min"));
        meditationList.add(new MeditationItem("Mindful Breathing", "Focus on breath", "15 min"));
        meditationList.add(new MeditationItem("Sleep Meditation", "Deep relaxation", "20 min"));
        meditationList.add(new MeditationItem("Stress Relief", "Release tension", "12 min"));

        // Set adapter
        adapter = new MeditationAdapter(meditationList);
        rvMeditations.setAdapter(adapter);

        btnMeditation.setOnClickListener(v -> {
            // Already on Meditation tab
        });

        btnBreathing.setOnClickListener(v -> {
            // Navigate to Breathing Activity
            Intent intent = new Intent(RelaxationActivity.this, BreathingActivity.class);
            startActivity(intent);
        });

        btnGoals.setOnClickListener(v -> {
            Intent intent = new Intent(RelaxationActivity.this, GoalsActivity.class);
            startActivity(intent);
        });
    }

    // Meditation Item Model Class
    public static class MeditationItem {
        private String title;
        private String subtitle;
        private String duration;

        public MeditationItem(String title, String subtitle, String duration) {
            this.title = title;
            this.subtitle = subtitle;
            this.duration = duration;
        }

        public String getTitle() {
            return title;
        }

        public String getSubtitle() {
            return subtitle;
        }

        public String getDuration() {
            return duration;
        }
    }

    // RecyclerView Adapter
    public class MeditationAdapter extends RecyclerView.Adapter<MeditationAdapter.ViewHolder> {
        private List<MeditationItem> items;

        public MeditationAdapter(List<MeditationItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_meditation_card, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            MeditationItem item = items.get(position);
            holder.tvTitle.setText(item.getTitle());
            holder.tvSubtitle.setText(item.getSubtitle());
            holder.tvDuration.setText(item.getDuration());

            // Click listener to open player activity
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(RelaxationActivity.this, MeditationPlayerActivity.class);
                intent.putExtra("title", item.getTitle());
                intent.putExtra("subtitle", item.getSubtitle());
                intent.putExtra("duration", item.getDuration());
                startActivity(intent);
            });

            // Play button click listener
            holder.ivPlayButton.setOnClickListener(v -> {
                Intent intent = new Intent(RelaxationActivity.this, MeditationPlayerActivity.class);
                intent.putExtra("title", item.getTitle());
                intent.putExtra("subtitle", item.getSubtitle());
                intent.putExtra("duration", item.getDuration());
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvSubtitle, tvDuration;
            ImageView ivPlayButton;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvMeditTitle);
                tvSubtitle = itemView.findViewById(R.id.tvMeditSubtitle);
                tvDuration = itemView.findViewById(R.id.tvMeditDuration);
                ivPlayButton = itemView.findViewById(R.id.tvMeditPlay);
            }
        }
    }
}
