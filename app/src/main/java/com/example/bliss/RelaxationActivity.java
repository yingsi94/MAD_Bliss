package com.example.bliss;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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

public class RelaxationActivity extends AppCompatActivity {

    private RecyclerView rvMeditations;
    private MeditationAdapter adapter;
    private List<MeditationItem> meditationList;
    private FloatingActionButton fabRelax;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_relaxation);

        // Initialize RecyclerView
        rvMeditations = findViewById(R.id.rvMeditations);
        rvMeditations.setLayoutManager(new LinearLayoutManager(this));

        // Create meditation items
        meditationList = new ArrayList<>();
        meditationList.add(new MeditationItem("Body Scan", "Progressive relaxation", "10 min"));
        meditationList.add(new MeditationItem("Body Scan", "Progressive relaxation", "10 min"));
        meditationList.add(new MeditationItem("Body Scan", "Progressive relaxation", "10 min"));
        meditationList.add(new MeditationItem("Body Scan", "Progressive relaxation", "10 min"));

        // Set adapter
        adapter = new MeditationAdapter(meditationList);
        rvMeditations.setAdapter(adapter);

        // Setup Bottom Navigation
        bottomNav = findViewById(R.id.bottomNavigation);

        // Setup Floating Action Button
        fabRelax = findViewById(R.id.fabRelax);
        fabRelax.setOnClickListener(v -> {
            Toast.makeText(this, "Relax clicked", Toast.LENGTH_SHORT).show();
            // You're already on Relax page, so just show a message
        });

        // Bottom Navigation Click Listeners
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                Toast.makeText(this, "Home clicked", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.nav_journal) {
                Toast.makeText(this, "Journal clicked", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.nav_relax) {
                // Do nothing, handled by FAB
                return false;
            } else if (itemId == R.id.nav_profile) {
                Toast.makeText(this, "Profile clicked", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
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

        public String getTitle() { return title; }
        public String getSubtitle() { return subtitle; }
        public String getDuration() { return duration; }
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

            holder.itemView.setOnClickListener(v ->
                    Toast.makeText(RelaxationActivity.this,
                            "Playing: " + item.getTitle(),
                            Toast.LENGTH_SHORT).show()
            );
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
