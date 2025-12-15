package com.example.bliss;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.cardview.widget.CardView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;


public class MusicSuggestionActivity extends AppCompatActivity implements SongAdapter.OnItemClickListener {

    // [Firebase Firestore Instance]
    private FirebaseFirestore db;

    // Controls
    private MediaPlayer mediaPlayer;
    private RecyclerView popularRecyclerView;
    private RecyclerView playlistRecyclerView;
    private RecyclerView searchRecyclerView;
    private TextView tvPlaylistTitle;
    private SearchView searchView;
    private ImageView btnToggleList;

    // ======== Bottom Playback Bar Controls and State ========
    private CardView nowPlayingBar;
    private TextView tvSongTitle;
    private TextView tvSongArtist;
    private ImageButton btnPlayPause; // 播放/暂停按钮

    // Progress Controls
    private SeekBar sbProgress;
    private TextView tvCurrentTime;
    private TextView tvTotalTime;

    // Progress Update Tools
    private final Handler handler = new Handler();
    private Runnable runnable;
    // ===================================

    // Data
    private final ArrayList<PlaylistModel> popularPlaylistList = new ArrayList<>();
    private final ArrayList<ModelClass> playlistSongs = new ArrayList<>();
    private final ArrayList<ModelClass> allSongs = new ArrayList<>();
    private final ArrayList<ModelClass> searchResults = new ArrayList<>();

    // Adapters
    private PlaylistAdapter popularAdapter;
    private SongAdapter playlistAdapter;
    private SongAdapter searchAdapter;

    private static final String PREFS_NAME = "MoodPrefs";
    private static final String KEY_SELECTED_MOOD = "selected_mood";
    private String currentMood = "happy"; // Default mood

    // State and Current Song Object
    private ModelClass currentlyPlayingSong = null;
    private boolean isPlaying = false;

    // ==================== [新增歌单数据和 Activity Result Launcher] ====================

    // 流行歌单名称 (用于 UI 显示)
    private final String[] POPULAR_PLAYLIST_NAMES = {
            "Happiness", "Chill", "Workout", "Study", "Party"
    };
    // 流行歌单在 Firestore 中对应的新筛选字段值 (用于查询 'playlist' 字段)
    private final String[] PLAYLIST_FILTER_VALUES = {
            "happiness",
            "chill",
            "workout",
            "study",
            "party"
    };

    // 用于接收 PlaylistSongActivity 返回结果的 Launcher
    private final ActivityResultLauncher<Intent> playlistSongLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null) {
                        // 接收并解析 PlaylistSongActivity 返回的歌曲数据
                        String songName = data.getStringExtra("SONG_NAME");
                        String singer = data.getStringExtra("SINGER");
                        String audioUrl = data.getStringExtra("AUDIO_URL");
                        int imgResId = data.getIntExtra("IMG_RES_ID", R.drawable.img_default);
                        String duration = data.getStringExtra("DURATION");

                        // 创建 ModelClass 实例
                        ModelClass selectedSong = new ModelClass();
                        selectedSong.setSong_name(songName);
                        selectedSong.setSinger(singer);
                        selectedSong.setAudioUrl(audioUrl);
                        selectedSong.setImg(imgResId);
                        selectedSong.setDuration(duration);

                        // 启动播放和更新 UI
                        handleSongSelectionFromPlaylist(selectedSong);
                    }
                }
            }
    );
    // ===================================================================================

    // [Helper Method] Formats milliseconds to minute:second
    private String formatDuration(int durationMs) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(minutes);
        seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) - TimeUnit.MINUTES.toSeconds(minutes);
        return String.format("%d:%02d", minutes, seconds);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_suggestion);

        // Initialize Firestore instance
        db = FirebaseFirestore.getInstance();

        initializeViews();
        setupListeners();

        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        getSelectedMood();
        setupPopularPlaylists(); // ★★★ 在这里配置了 Launcher 启动 ★★★

        fetchSongsFromFirestore();

        setupSearchFunction();
        setupToggleButton();
    }

    // ==================== 1. Initialization and Listeners ====================

    private void initializeViews() {
        popularRecyclerView = findViewById(R.id.popularRecyclerView);
        playlistRecyclerView = findViewById(R.id.playlistRecyclerView);
        searchRecyclerView = findViewById(R.id.searchRecyclerView);
        tvPlaylistTitle = findViewById(R.id.tv_playlist_title);
        searchView = findViewById(R.id.searchView);
        btnToggleList = findViewById(R.id.btn_toggle_list);

        nowPlayingBar = findViewById(R.id.nowPlayingBar);
        tvSongTitle = findViewById(R.id.tv_song_title);
        tvSongArtist = findViewById(R.id.tv_song_artist);

        // Progress Controls
        sbProgress = findViewById(R.id.sb_progress);
        tvCurrentTime = findViewById(R.id.tv_current_time);
        tvTotalTime = findViewById(R.id.tv_total_time);

        // [Playback Controls Initialization]
        btnPlayPause = findViewById(R.id.btn_play_pause);

        nowPlayingBar.setVisibility(View.GONE);
    }

    private void setupListeners() {
        btnPlayPause.setOnClickListener(v -> togglePlayPause());

        // 1. Progress SeekBar Listener (Unchanged)
        sbProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    tvCurrentTime.setText(formatDuration(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                handler.removeCallbacks(runnable);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mediaPlayer != null) {
                    mediaPlayer.seekTo(seekBar.getProgress());
                    tvCurrentTime.setText(formatDuration(mediaPlayer.getCurrentPosition()));
                    updateProgressBar();
                }
            }
        });
    }

    // ==================== 2. Data Loading and List Setup ====================

    private void setupPopularPlaylists() {
        popularPlaylistList.clear();

        int[] popularPlaylistImages = {
                R.drawable.playlist_happiness, R.drawable.playlist_chill,
                R.drawable.playlist_workout, R.drawable.playlist_study,
                R.drawable.playlist_party
        };

        for (int playlistImage : popularPlaylistImages) {
            popularPlaylistList.add(new PlaylistModel(playlistImage));
        }

        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        popularRecyclerView.setLayoutManager(layoutManager);

        // ★★★ 关键修改：点击时启动 PlaylistSongActivity 并传递新的筛选 Key ★★★
        popularAdapter = new PlaylistAdapter(this, popularPlaylistList, (position, playlist) -> {

            if (position >= POPULAR_PLAYLIST_NAMES.length) return;

            String selectedPlaylistName = POPULAR_PLAYLIST_NAMES[position];
            String selectedFilterValue = PLAYLIST_FILTER_VALUES[position];

            Intent intent = new Intent(MusicSuggestionActivity.this, PlaylistSongActivity.class);

            // 传递歌单标题 (PlaylistSongActivity 中用于 UI)
            intent.putExtra("PLAYLIST_TITLE", selectedPlaylistName);

            // 传递新的筛选值 (PlaylistSongActivity 中用于 Firebase 查询 'playlist' 字段)
            intent.putExtra("PLAYLIST_FILTER_KEY", selectedFilterValue);

            // 使用 Launcher 启动 Activity
            playlistSongLauncher.launch(intent);

            Toast.makeText(this, "Opening: " + selectedPlaylistName + " Playlist", Toast.LENGTH_SHORT).show();
        });
        popularRecyclerView.setAdapter(popularAdapter);
    }

    /**
     * Asynchronously fetches song data from Firebase Firestore
     */
    private void fetchSongsFromFirestore() {
        allSongs.clear();

        Toast.makeText(this, "Loading songs from cloud...", Toast.LENGTH_SHORT).show();

        db.collection("songs")
                // Order by song name for a stable default order
                .orderBy("songName")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                // 1. Read data (using Firestore field names)
                                String songName = document.getString("songName");
                                String singer = document.getString("singer");
                                String duration = document.getString("duration");
                                String audioUrl = document.getString("audioUrl");
                                String imgFileName = document.getString("imgFileName");
                                // Read mood field
                                String moodTag = document.getString("mood");

                                // 2. Convert filename to R.drawable ID
                                int imageResId = getResourceIdForImage(imgFileName);

                                // 3. Create ModelClass
                                ModelClass song = new ModelClass();
                                song.setSong_name(songName);
                                song.setSinger(singer);
                                song.setDuration(duration != null ? duration : "N/A");
                                song.setAudioUrl(audioUrl);
                                song.setImg(imageResId);
                                // Set mood field
                                song.setMood(moodTag); // 保留 mood 字段，用于 setupPlaylistForYou()

                                allSongs.add(song);
                            } catch (Exception e) {
                                Log.e("FirestoreFetch", "Error parsing song document: " + document.getId(), e);
                            }
                        }

                        // After data loading, initialize and populate the list UI
                        setupPlaylistForYou();
                        setupSearchRecyclerView();

                        Toast.makeText(this, "Songs loaded successfully!", Toast.LENGTH_SHORT).show();

                    } else {
                        Log.w("FirestoreFetch", "Error getting documents.", task.getException());
                        Toast.makeText(this, "Failed to load songs. Check Firestore connection.", Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Helper method: Dynamically gets the R.drawable int ID using the resource filename string.
     */
    private int getResourceIdForImage(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            // Ensure you have a default image R.drawable.img_default
            return R.drawable.img_default;
        }

        // Use getIdentifier to find the resource in the R.drawable namespace
        int resourceId = getResources().getIdentifier(
                fileName,
                "drawable", // Resource type is drawable
                getPackageName() // Your application's package name
        );

        if (resourceId == 0) {
            // If the resource is not found, return the default image
            return R.drawable.img_default;
        }

        return resourceId;
    }

    /**
     * Uses the mood tag to filter songs for recommendation (Unchanged)
     */
    private void setupPlaylistForYou() {
        playlistSongs.clear();

        if (allSongs.isEmpty()) {
            Log.w("SetupList", "Song list is empty. Cannot setup playlist.");
            return;
        }

        // Determine the target mood tag and convert to lowercase
        String targetMood = currentMood.toLowerCase();

        int songsAdded = 0;
        final int MAX_SONGS = 3; // Recommend only 3 songs

        // Iterate through all songs and filter those matching the current mood
        for (ModelClass song : allSongs) {
            if (song.getMood() != null && song.getMood().equalsIgnoreCase(targetMood)) {
                playlistSongs.add(song);
                songsAdded++;

                // Stop immediately when the recommendation limit is reached
                if (songsAdded >= MAX_SONGS) {
                    break;
                }
            }
        }

        // Set up RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        playlistRecyclerView.setLayoutManager(layoutManager);

        if (playlistSongs.isEmpty()) {
            Log.w("SetupList", "No songs found for mood: " + targetMood);
            // You can add a default empty state message here
        }

        playlistAdapter = new SongAdapter(this, playlistSongs);
        playlistRecyclerView.setAdapter(playlistAdapter);

        playlistAdapter.setOnItemClickListener(this);
    }

    private void setupSearchRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        searchRecyclerView.setLayoutManager(layoutManager);
        searchRecyclerView.setNestedScrollingEnabled(true);
        searchRecyclerView.setHasFixedSize(false);

        searchAdapter = new SongAdapter(this, searchResults);
        searchRecyclerView.setAdapter(searchAdapter);

        searchAdapter.setOnItemClickListener(this);
    }

    // ==================== 3. Song Click and Playback Control ====================

    @Override
    public void onItemClick(int position, ModelClass song) {
        // 这是点击“为您推荐”列表或搜索结果的逻辑
        hideSearchList();

        loadSongToPlayerBar(song);

        startNewSong(song);

        Toast.makeText(this, "Playing: " + song.getSong_name(), Toast.LENGTH_SHORT).show();
    }

    /**
     * 集中处理从 PlaylistSongActivity 返回的歌曲数据，并启动播放
     */
    private void handleSongSelectionFromPlaylist(ModelClass song) {
        hideSearchList();
        loadSongToPlayerBar(song);

        if (song.getAudioUrl() != null && !song.getAudioUrl().isEmpty()) {
            startNewSong(song); // 启动播放
        } else {
            Toast.makeText(this, "Audio URL is missing or invalid!", Toast.LENGTH_LONG).show();
        }
    }


    private void loadSongToPlayerBar(ModelClass song) {
        currentlyPlayingSong = song;
        tvSongTitle.setText(song.getSong_name());
        tvSongArtist.setText(song.getSinger());

        if (nowPlayingBar.getVisibility() != View.VISIBLE) {
            nowPlayingBar.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Starts playing a new song using the network URL (Cloudinary) and asynchronous prepareAsync.
     */
    private void startNewSong(ModelClass song) {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
            handler.removeCallbacks(runnable);
        }

        String audioUrl = song.getAudioUrl();
        Log.d("MUSIC_PLAYER", "Attempting to play URL: " + audioUrl);

        if (audioUrl == null || audioUrl.isEmpty()) {
            Toast.makeText(this, "Audio URL is invalid or missing!", Toast.LENGTH_SHORT).show();
            isPlaying = false;
            updatePlayPauseButton();
            return;
        }

        try {
            mediaPlayer = new MediaPlayer();
            // 在 prepareAsync 之前设置数据源
            mediaPlayer.setDataSource(audioUrl);

            mediaPlayer.setOnCompletionListener(mp -> {
                isPlaying = false;
                updatePlayPauseButton();
                handler.removeCallbacks(runnable);
                sbProgress.setProgress(0);
                tvCurrentTime.setText("0:00");
            });

            mediaPlayer.setOnPreparedListener(mp -> {
                int duration = mp.getDuration();
                tvTotalTime.setText(formatDuration(duration));
                sbProgress.setMax(duration);
                sbProgress.setProgress(0);

                mp.start();
                isPlaying = true;
                updatePlayPauseButton();
                updateProgressBar();
                Log.d("MUSIC_PLAYER", "Playback started successfully.");
            });

            mediaPlayer.prepareAsync();

            // 立即更新 UI 以显示缓冲状态，避免白屏感
            tvTotalTime.setText("Buffering...");

        } catch (IOException e) {
            Log.e("MUSIC_PLAYER", "Failed to set data source or prepareAsync.", e);
            Toast.makeText(this, "Failed to load network audio: " + e.getMessage(), Toast.LENGTH_LONG).show();
            mediaPlayer = null;
            isPlaying = false;
            updatePlayPauseButton();
        } catch (IllegalStateException e) {
            Log.e("MUSIC_PLAYER", "Illegal state during playback preparation.", e);
            mediaPlayer = null;
            isPlaying = false;
            updatePlayPauseButton();
        }
    }

    private void togglePlayPause() {
        if (mediaPlayer == null) {
            if (currentlyPlayingSong != null) {
                Toast.makeText(this, "Reloading song...", Toast.LENGTH_SHORT).show();
                startNewSong(currentlyPlayingSong);
            }
            return;
        }

        try {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                isPlaying = false;
                handler.removeCallbacks(runnable);
            } else {
                mediaPlayer.start();
                isPlaying = true;
                updateProgressBar();
            }
        } catch (IllegalStateException e) {
            Log.w("MUSIC_PLAYER", "Toggle clicked while player is preparing.");
            return;
        }

        updatePlayPauseButton();
    }


    // ==================== 4. Helper Methods ====================

    private void updatePlayPauseButton() {
        if (isPlaying) {
            btnPlayPause.setImageResource(R.drawable.btn_pause_normal);
        } else {
            btnPlayPause.setImageResource(R.drawable.btn_play_normal);
        }
    }

    private void updateProgressBar() {
        runnable = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    int currentPosition = mediaPlayer.getCurrentPosition();

                    sbProgress.setProgress(currentPosition);
                    tvCurrentTime.setText(formatDuration(currentPosition));

                    handler.postDelayed(this, 100);
                }
            }
        };
        handler.post(runnable);
    }

    private void getSelectedMood() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        currentMood = prefs.getString(KEY_SELECTED_MOOD, "happy");
        updatePlaylistTitle();
    }

    private void updatePlaylistTitle() {
        String title = "Playlist for you";
        String subtitle = "";

        switch (currentMood) {
            case "calm": subtitle = "Calm Mood"; break;
            case "sad": subtitle = "Sad Mood"; break;
            case "stressed": subtitle = "Stressed Mood"; break;
            case "happy":
            default: subtitle = "Happy Mood"; break;
        }

        tvPlaylistTitle.setText(title + " - " + subtitle);
    }

    private void setupSearchFunction() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String query) {
                performSearch(query);
                if (query.isEmpty()) {
                    showSearchList();
                }
                return true;
            }

            @Override
            public boolean onQueryTextSubmit(String query) {
                performSearch(query);
                return true;
            }
        });

        searchView.setOnCloseListener(() -> {
            hideSearchList();
            return false;
        });
    }

    private void setupToggleButton() {
        btnToggleList.setOnClickListener(v -> {
            if (searchRecyclerView.getVisibility() == View.VISIBLE) {
                hideSearchList();
            } else {
                showSearchList();
                performSearch("");
                searchView.requestFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(searchView.findFocus(), 0);
                }
            }
        });
    }

    private int getMoodIndex(String mood) {
        switch (mood) {
            case "calm": return 1;
            case "sad": return 2;
            case "stressed": return 3;
            case "happy":
            default: return 0;
        }
    }

    private void performSearch(String query) {
        searchResults.clear();

        if (query == null || query.trim().isEmpty()) {
            searchResults.addAll(allSongs);
        } else {
            String searchText = query.toLowerCase().trim();
            for (ModelClass song : allSongs) {
                if (song.getSong_name().toLowerCase().contains(searchText) ||
                        song.getSinger().toLowerCase().contains(searchText)) {
                    searchResults.add(song);
                }
            }
        }
        searchAdapter.notifyDataSetChanged();
    }

    private void showSearchList() {
        if (searchRecyclerView.getVisibility() != View.VISIBLE) {
            searchRecyclerView.setVisibility(View.VISIBLE);
            searchRecyclerView.bringToFront();
        }
    }

    private void hideSearchList() {
        searchRecyclerView.setVisibility(View.GONE);
        searchView.setQuery("", false);
        searchResults.clear();
        searchAdapter.notifyDataSetChanged();

        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    // ==================== 5. Lifecycle Management ====================

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (runnable != null) {
            handler.removeCallbacks(runnable);
        }
    }
}