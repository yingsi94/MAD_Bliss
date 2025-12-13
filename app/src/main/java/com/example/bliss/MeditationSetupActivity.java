package com.example.bliss;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

// Cloud Firestore Imports
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class MeditationSetupActivity extends AppCompatActivity {

    private static final String TAG = "MeditationSetupActivity";

    private NumberPicker npMinutes, npSeconds;
    private RadioGroup rgMusic;
    private RadioButton rbNoMusic;
    private RecyclerView rvMusicOptions, rvThemes;
    private SwitchCompat switchBreathingGuide;
    private Button btnStartMeditation;
    private ImageView btnBack;

    private MusicAdapter musicAdapter;
    private ThemeAdapter themeAdapter;
    private List<MusicItem> musicList;
    private List<ThemeItem> themeList;

    private FirebaseFirestore firestore;

    private String selectedMusicId = null;
    private String selectedMusicName = "No Music";
    private String selectedMusicUrl = null;
    private String selectedThemeId = null;
    private String selectedThemeName = null;
    private List<String> selectedThemeTexts = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meditation_setup);

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        // Initialize views
        npMinutes = findViewById(R.id.npMinutes);
        npSeconds = findViewById(R.id.npSeconds);
        rgMusic = findViewById(R.id.rgMusic);
        rbNoMusic = findViewById(R.id.rbNoMusic);
        rvMusicOptions = findViewById(R.id.rvMusicOptions);
        rvThemes = findViewById(R.id.rvThemes);
        switchBreathingGuide = findViewById(R.id.switchBreathingGuide);
        btnStartMeditation = findViewById(R.id.btnStartMeditation);
        btnBack = findViewById(R.id.btnBack);

        // Setup Number Pickers
        setupNumberPickers();

        // Setup RecyclerViews
        setupRecyclerViews();

        // Call Firestore loading methods
        loadMusicFromFirestore();
        loadThemesFromFirestore();

        // Setup listeners
        setupListeners();
    }

    private void setupNumberPickers() {
        // Minutes picker (0-60)
        npMinutes.setMinValue(0);
        npMinutes.setMaxValue(60);
        npMinutes.setValue(10); // Default 10 minutes
        npMinutes.setWrapSelectorWheel(true);

        // Seconds picker (0-59)
        npSeconds.setMinValue(0);
        npSeconds.setMaxValue(59);
        npSeconds.setValue(0);
        npSeconds.setWrapSelectorWheel(true);
    }

    private void setupRecyclerViews() {
        // Music RecyclerView
        musicList = new ArrayList<>();
        musicAdapter = new MusicAdapter(musicList);
        rvMusicOptions.setLayoutManager(new LinearLayoutManager(this));
        rvMusicOptions.setAdapter(musicAdapter);

        // Theme RecyclerView
        themeList = new ArrayList<>();
        themeAdapter = new ThemeAdapter(themeList);
        rvThemes.setLayoutManager(new LinearLayoutManager(this));
        rvThemes.setAdapter(themeAdapter);
    }

    private void loadMusicFromFirestore() {
        firestore.collection("meditation_music")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            musicList.clear();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                MusicItem music = document.toObject(MusicItem.class);
                                music.setId(document.getId());
                                musicList.add(music);
                            }
                            musicAdapter.notifyDataSetChanged();
                        } else {
                            Log.w(TAG, "Error getting music documents.", task.getException());
                            Toast.makeText(MeditationSetupActivity.this,
                                    "Failed to load music.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void loadThemesFromFirestore() {
        firestore.collection("meditation_themes")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            themeList.clear();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                ThemeItem theme = document.toObject(ThemeItem.class);
                                theme.setId(document.getId());
                                themeList.add(theme);
                            }
                            themeAdapter.notifyDataSetChanged();

                            // Auto-select first theme if available
                            if (!themeList.isEmpty() && selectedThemeId == null) {
                                selectTheme(themeList.get(0));
                            }

                            // FIX: Force RecyclerView to recalculate height
                            rvThemes.post(() -> {
                                themeAdapter.notifyDataSetChanged();
                                rvThemes.requestLayout();
                            });
                        } else {
                            Log.w(TAG, "Error getting theme documents.", task.getException());
                            Toast.makeText(MeditationSetupActivity.this,
                                    "Failed to load themes.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        rbNoMusic.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedMusicId = null;
                selectedMusicName = "No Music";
                selectedMusicUrl = null;
                musicAdapter.clearSelection();
            }
        });

        btnStartMeditation.setOnClickListener(v -> {
            int minutes = npMinutes.getValue();
            int seconds = npSeconds.getValue();
            int totalSeconds = (minutes * 60) + seconds;

            if (totalSeconds == 0) {
                Toast.makeText(this, "Please select a duration", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedThemeId == null) {
                Toast.makeText(this, "Please select a theme", Toast.LENGTH_SHORT).show();
                return;
            }

            // Start meditation player activity
            Intent intent = new Intent(MeditationSetupActivity.this, MeditationPlayerActivity.class);
            intent.putExtra("duration", totalSeconds);
            intent.putExtra("musicId", selectedMusicId);
            intent.putExtra("musicName", selectedMusicName);
            intent.putExtra("musicUrl", selectedMusicUrl);
            intent.putExtra("themeId", selectedThemeId);
            intent.putExtra("themeName", selectedThemeName);
            intent.putStringArrayListExtra("themeTexts",
                    selectedThemeTexts != null ? new ArrayList<>(selectedThemeTexts) : new ArrayList<>());
            intent.putExtra("breathingGuide", switchBreathingGuide.isChecked());
            startActivity(intent);
        });
    }

    private void selectMusic(MusicItem music) {
        selectedMusicId = music.getId();
        selectedMusicName = music.getName();
        selectedMusicUrl = music.getUrl();
        rbNoMusic.setChecked(false);
    }

    private void selectTheme(ThemeItem theme) {
        selectedThemeId = theme.getId();
        selectedThemeName = theme.getName();
        selectedThemeTexts = theme.getTexts();

        // Ensure ThemeAdapter updates its selection visually
        int position = themeList.indexOf(theme);
        if (position != -1) {
            themeAdapter.setSelectedPosition(position);
        }
    }

    // Music Item Model
    public static class MusicItem {
        private String id;
        private String name;
        private String url;

        public MusicItem() {}

        public MusicItem(String name, String url) {
            this.name = name;
            this.url = url;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }

    // Theme Item Model
    public static class ThemeItem {
        private String id;
        private String name;
        private String description;
        private List<String> texts;

        public ThemeItem() {}

        public ThemeItem(String name, String description, List<String> texts) {
            this.name = name;
            this.description = description;
            this.texts = texts;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public List<String> getTexts() { return texts; }
        public void setTexts(List<String> texts) { this.texts = texts; }
    }

    // Music Adapter
    private class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.ViewHolder> {
        private List<MusicItem> items;
        private int selectedPosition = -1;

        public MusicAdapter(List<MusicItem> items) {
            this.items = items;
        }

        public void clearSelection() {
            selectedPosition = -1;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_music_option, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            MusicItem item = items.get(position);
            holder.rbMusic.setText(item.getName());
            holder.rbMusic.setChecked(position == selectedPosition);

            View.OnClickListener clickListener = v -> {
                int previousSelected = selectedPosition;
                selectedPosition = holder.getAdapterPosition();
                selectMusic(item);

                notifyItemChanged(previousSelected);
                notifyItemChanged(selectedPosition);
            };

            holder.itemView.setOnClickListener(clickListener);
            holder.rbMusic.setOnClickListener(clickListener);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            RadioButton rbMusic;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                rbMusic = itemView.findViewById(R.id.rbMusicOption);
            }
        }
    }

    // Theme Adapter - FIXED VERSION
    private class ThemeAdapter extends RecyclerView.Adapter<ThemeAdapter.ViewHolder> {
        private List<ThemeItem> items;
        private int selectedPosition = 0;

        public ThemeAdapter(List<ThemeItem> items) {
            this.items = items;
        }

        public void setSelectedPosition(int position) {
            int previousSelected = selectedPosition;
            selectedPosition = position;
            notifyItemChanged(previousSelected);
            notifyItemChanged(selectedPosition);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_theme_option, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ThemeItem item = items.get(position);
            holder.rbTheme.setText(item.getName());
            holder.tvDescription.setText(item.getDescription());
            holder.rbTheme.setChecked(position == selectedPosition);

            View.OnClickListener clickListener = v -> {
                int previousSelected = selectedPosition;
                selectedPosition = holder.getAdapterPosition();
                selectTheme(item);

                notifyItemChanged(previousSelected);
                notifyItemChanged(selectedPosition);
            };

            holder.itemView.setOnClickListener(clickListener);
            holder.rbTheme.setOnClickListener(clickListener);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            RadioButton rbTheme;
            TextView tvDescription;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                rbTheme = itemView.findViewById(R.id.rbThemeOption);
                tvDescription = itemView.findViewById(R.id.tvThemeDescription);
            }
        }
    }
}