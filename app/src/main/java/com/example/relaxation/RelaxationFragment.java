package com.example.relaxation;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bliss.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class RelaxationFragment extends Fragment {

    private static final String TAG = "RelaxationFragment";

    private Button btnMeditation, btnBreathing, btnGoals, btnStartMeditation;
    private NumberPicker npMinutes, npSeconds;
    private RadioGroup rgMusic;
    private RadioButton rbNoMusic;
    private RecyclerView rvMusicOptions, rvThemes;
    private SwitchCompat switchBreathingGuide;

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

    public RelaxationFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 使用你的布局文件
        return inflater.inflate(R.layout.fragment_relaxation, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        initializeViews(view);
        setupNumberPickers();
        setupRecyclerViews();
        loadMusicFromFirestore();
        loadThemesFromFirestore();
        setupListeners();
    }

    private void initializeViews(View view) {
        btnMeditation = view.findViewById(R.id.btnMeditation);
        btnBreathing = view.findViewById(R.id.btnBreathing);
        btnGoals = view.findViewById(R.id.btnGoals);
        btnStartMeditation = view.findViewById(R.id.btnStartMeditation);

        npMinutes = view.findViewById(R.id.npMinutes);
        npSeconds = view.findViewById(R.id.npSeconds);
        rgMusic = view.findViewById(R.id.rgMusic);
        rbNoMusic = view.findViewById(R.id.rbNoMusic);
        rvMusicOptions = view.findViewById(R.id.rvMusicOptions);
        rvThemes = view.findViewById(R.id.rvThemes);
        switchBreathingGuide = view.findViewById(R.id.switchBreathingGuide);
    }

    @Override
    public void onResume() {
        super.onResume();
        selectTab(btnMeditation);
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

    private void setupNumberPickers() {
        npMinutes.setMinValue(0);
        npMinutes.setMaxValue(60);
        npMinutes.setValue(10);
        npMinutes.setWrapSelectorWheel(true);

        npSeconds.setMinValue(0);
        npSeconds.setMaxValue(59);
        npSeconds.setValue(0);
        npSeconds.setWrapSelectorWheel(true);
    }

    private void setupRecyclerViews() {
        musicList = new ArrayList<>();
        musicAdapter = new MusicAdapter(musicList);
        rvMusicOptions.setLayoutManager(new LinearLayoutManager(getContext()));
        rvMusicOptions.setAdapter(musicAdapter);

        themeList = new ArrayList<>();
        themeAdapter = new ThemeAdapter(themeList);
        rvThemes.setLayoutManager(new LinearLayoutManager(getContext()));
        rvThemes.setAdapter(themeAdapter);
    }

    private void loadMusicFromFirestore() {
        firestore.collection("meditation_music")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful() && isAdded()) {
                            musicList.clear();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                MusicItem music = document.toObject(MusicItem.class);
                                music.setId(document.getId());
                                musicList.add(music);
                            }
                            musicAdapter.notifyDataSetChanged();
                        } else {
                            Log.w(TAG, "Error getting music documents.", task.getException());
                            if (isAdded()) {
                                Toast.makeText(getContext(), "Failed to load music.", Toast.LENGTH_SHORT).show();
                            }
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
                        if (task.isSuccessful() && isAdded()) {
                            themeList.clear();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                ThemeItem theme = document.toObject(ThemeItem.class);
                                theme.setId(document.getId());
                                themeList.add(theme);
                            }
                            themeAdapter.notifyDataSetChanged();

                            if (!themeList.isEmpty() && selectedThemeId == null) {
                                selectTheme(themeList.get(0));
                            }

                            rvThemes.post(() -> {
                                if (isAdded()) {
                                    themeAdapter.notifyDataSetChanged();
                                    rvThemes.requestLayout();
                                }
                            });
                        } else {
                            Log.w(TAG, "Error getting theme documents.", task.getException());
                            if (isAdded()) {
                                Toast.makeText(getContext(), "Failed to load themes.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
    }

    private void setupListeners() {
        // 1. 跳转到呼吸训练 (BreathingFragment)
        btnBreathing.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new BreathingFragment()) // 替换为呼吸页面
                    .addToBackStack(null) // 重要：这样按返回键能回到 Relaxation 选择页
                    .commit();
        });

        // 2. 跳转到目标页面 (GoalsFragment)
        btnGoals.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new GoalsFragment()) // 替换为目标页面
                    .addToBackStack(null) // 重要
                    .commit();
        });

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
                Toast.makeText(getContext(), "Please select a duration", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedThemeId == null) {
                Toast.makeText(getContext(), "Please select a theme", Toast.LENGTH_SHORT).show();
                return;
            }

            MeditationPlayerFragment playerFragment = new MeditationPlayerFragment();
            Bundle args = new Bundle();
            args.putInt("duration", totalSeconds);
            args.putString("musicId", selectedMusicId);
            args.putString("musicName", selectedMusicName);
            args.putString("musicUrl", selectedMusicUrl);
            args.putString("themeId", selectedThemeId);
            args.putString("themeName", selectedThemeName);
            args.putStringArrayList("themeTexts",
                    selectedThemeTexts != null ? new ArrayList<>(selectedThemeTexts) : new ArrayList<>());
            args.putBoolean("breathingGuide", switchBreathingGuide.isChecked());
            playerFragment.setArguments(args);

            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, playerFragment)
                    .addToBackStack(null)
                    .commit();
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
        public MusicItem(String name, String url) { this.name = name; this.url = url; }
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
            this.name = name; this.description = description; this.texts = texts;
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

        public MusicAdapter(List<MusicItem> items) { this.items = items; }
        public void clearSelection() { selectedPosition = -1; notifyDataSetChanged(); }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_music_option, parent, false);
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
        public int getItemCount() { return items.size(); }
        class ViewHolder extends RecyclerView.ViewHolder {
            RadioButton rbMusic;
            ViewHolder(@NonNull View itemView) { super(itemView); rbMusic = itemView.findViewById(R.id.rbMusicOption); }
        }
    }

    // Theme Adapter
    private class ThemeAdapter extends RecyclerView.Adapter<ThemeAdapter.ViewHolder> {
        private List<ThemeItem> items;
        private int selectedPosition = 0;

        public ThemeAdapter(List<ThemeItem> items) { this.items = items; }
        public void setSelectedPosition(int position) {
            int previousSelected = selectedPosition;
            selectedPosition = position;
            notifyItemChanged(previousSelected);
            notifyItemChanged(selectedPosition);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_theme_option, parent, false);
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
        public int getItemCount() { return items.size(); }
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