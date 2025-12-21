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

    private FirebaseFirestore db;
    private MediaPlayer mediaPlayer;
    private RecyclerView popularRecyclerView, playlistRecyclerView, searchRecyclerView;
    private TextView tvPlaylistTitle, tvSongTitle, tvSongArtist, tvCurrentTime, tvTotalTime;
    private SearchView searchView;
    private ImageView btnToggleList;
    private CardView nowPlayingBar;
    private ImageButton btnPlayPause;
    private SeekBar sbProgress;

    private final Handler handler = new Handler();
    private Runnable runnable;

    private final ArrayList<PlaylistModel> popularPlaylistList = new ArrayList<>();
    private final ArrayList<ModelClass> playlistSongs = new ArrayList<>();
    private final ArrayList<ModelClass> allSongs = new ArrayList<>();
    private final ArrayList<ModelClass> searchResults = new ArrayList<>();

    private PlaylistAdapter popularAdapter;
    private SongAdapter playlistAdapter, searchAdapter;

    private static final String PREFS_NAME = "MoodPrefs";
    private static final String KEY_SELECTED_MOOD = "selected_mood";
    private String currentMood = "happy";

    private ModelClass currentlyPlayingSong = null;
    private boolean isPlaying = false;
    private boolean isPlayerReady = false; // 新增：防止异步准备未完成时点击

    private final String[] POPULAR_PLAYLIST_NAMES = {"Happiness", "Chill", "Workout", "Study", "Party"};
    private final String[] PLAYLIST_FILTER_VALUES = {"happiness", "chill", "workout", "study", "party"};

    private final ActivityResultLauncher<Intent> playlistSongLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    ModelClass selectedSong = new ModelClass();
                    selectedSong.setSong_name(data.getStringExtra("SONG_NAME"));
                    selectedSong.setSinger(data.getStringExtra("SINGER"));
                    selectedSong.setAudioUrl(data.getStringExtra("AUDIO_URL"));
                    selectedSong.setImg(data.getIntExtra("IMG_RES_ID", R.drawable.img_default));
                    selectedSong.setDuration(data.getStringExtra("DURATION"));
                    handleSongSelectionFromPlaylist(selectedSong);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_suggestion);

        db = FirebaseFirestore.getInstance();

        initializeViews();
        setupListeners();

        // 1. 获取心情（优先从 Intent 获取实时值）
        getSelectedMood();

        // 2. 初始化 UI
        setupPopularPlaylists();

        // 3. 从 Firestore 加载歌曲并根据心情过滤
        fetchSongsFromFirestore();

        setupSearchFunction();
        setupToggleButton();

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

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
        sbProgress = findViewById(R.id.sb_progress);
        tvCurrentTime = findViewById(R.id.tv_current_time);
        tvTotalTime = findViewById(R.id.tv_total_time);
        btnPlayPause = findViewById(R.id.btn_play_pause);

        nowPlayingBar.setVisibility(View.GONE);
    }

    private void getSelectedMood() {
        // 【关键】优先获取 TrackMoodActivity 传过来的心情
        String moodFromIntent = getIntent().getStringExtra("SELECTED_MOOD");
        if (moodFromIntent != null && !moodFromIntent.isEmpty()) {
            currentMood = moodFromIntent;
        } else {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            currentMood = prefs.getString(KEY_SELECTED_MOOD, "happy");
        }
        updatePlaylistTitle();
    }

    private void fetchSongsFromFirestore() {
        allSongs.clear();
        db.collection("songs").orderBy("songName").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    ModelClass song = new ModelClass();
                    song.setSong_name(document.getString("songName"));
                    song.setSinger(document.getString("singer"));
                    song.setDuration(document.getString("duration"));
                    song.setAudioUrl(document.getString("audioUrl"));
                    song.setImg(getResourceIdForImage(document.getString("imgFileName")));
                    song.setMood(document.getString("mood"));
                    allSongs.add(song);
                }
                setupPlaylistForYou(); // 加载完后立即过滤推荐
                setupSearchRecyclerView();
            }
        });
    }

    private void setupPlaylistForYou() {
        playlistSongs.clear();
        String targetMood = currentMood.toLowerCase();
        int count = 0;
        for (ModelClass song : allSongs) {
            if (song.getMood() != null && song.getMood().equalsIgnoreCase(targetMood)) {
                playlistSongs.add(song);
                count++;
                if (count >= 3) break; // 推荐3首
            }
        }
        playlistRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        playlistAdapter = new SongAdapter(this, playlistSongs);
        playlistRecyclerView.setAdapter(playlistAdapter);
        playlistAdapter.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(int position, ModelClass song) {
        hideSearchList();
        loadSongToPlayerBar(song);
        startNewSong(song);
    }

    private void startNewSong(ModelClass song) {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
            handler.removeCallbacks(runnable);
        }

        isPlayerReady = false;
        String audioUrl = song.getAudioUrl();
        if (audioUrl == null || audioUrl.isEmpty()) return;

        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(audioUrl);
            mediaPlayer.setOnPreparedListener(mp -> {
                isPlayerReady = true;
                tvTotalTime.setText(formatDuration(mp.getDuration()));
                sbProgress.setMax(mp.getDuration());
                mp.start();
                isPlaying = true;
                updatePlayPauseButton();
                updateProgressBar();
            });
            mediaPlayer.setOnCompletionListener(mp -> {
                isPlaying = false;
                updatePlayPauseButton();
            });
            tvTotalTime.setText("Buffering...");
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            Log.e("PLAYER", "Error", e);
        }
    }

    private void togglePlayPause() {
        if (mediaPlayer != null && isPlayerReady) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                isPlaying = false;
            } else {
                mediaPlayer.start();
                isPlaying = true;
                updateProgressBar();
            }
            updatePlayPauseButton();
        }
    }

    private void updatePlayPauseButton() {
        btnPlayPause.setImageResource(isPlaying ? R.drawable.btn_pause_normal : R.drawable.btn_play_normal);
    }

    private void updateProgressBar() {
        runnable = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && isPlaying) {
                    sbProgress.setProgress(mediaPlayer.getCurrentPosition());
                    tvCurrentTime.setText(formatDuration(mediaPlayer.getCurrentPosition()));
                    handler.postDelayed(this, 500);
                }
            }
        };
        handler.post(runnable);
    }

    private String formatDuration(int ms) {
        return String.format("%d:%02d", TimeUnit.MILLISECONDS.toMinutes(ms),
                TimeUnit.MILLISECONDS.toSeconds(ms) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(ms)));
    }

    // ... 其他辅助方法 (initializeViews, setupPopularPlaylists, setupSearchFunction, getResourceIdForImage 等保持原样)

    private void setupListeners() {
        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        sbProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) tvCurrentTime.setText(formatDuration(progress));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { handler.removeCallbacks(runnable); }
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                if (mediaPlayer != null && isPlayerReady) {
                    mediaPlayer.seekTo(seekBar.getProgress());
                    updateProgressBar();
                }
            }
        });
    }

    private void loadSongToPlayerBar(ModelClass song) {
        currentlyPlayingSong = song;
        tvSongTitle.setText(song.getSong_name());
        tvSongArtist.setText(song.getSinger());
        nowPlayingBar.setVisibility(View.VISIBLE);
    }

    private void handleSongSelectionFromPlaylist(ModelClass song) {
        loadSongToPlayerBar(song);
        startNewSong(song);
    }

    private void updatePlaylistTitle() {
        String subtitle = currentMood.substring(0,1).toUpperCase() + currentMood.substring(1).toLowerCase() + " Mood";
        tvPlaylistTitle.setText("Playlist for you - " + subtitle);
    }

    private int getResourceIdForImage(String fileName) {
        int resId = getResources().getIdentifier(fileName, "drawable", getPackageName());
        return resId != 0 ? resId : R.drawable.img_default;
    }

    private void setupPopularPlaylists() {
        int[] images = {R.drawable.playlist_happiness, R.drawable.playlist_chill, R.drawable.playlist_workout, R.drawable.playlist_study, R.drawable.playlist_party};
        for (int img : images) popularPlaylistList.add(new PlaylistModel(img));
        popularRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        popularAdapter = new PlaylistAdapter(this, popularPlaylistList, (pos, p) -> {
            Intent intent = new Intent(this, PlaylistSongActivity.class);
            intent.putExtra("PLAYLIST_TITLE", POPULAR_PLAYLIST_NAMES[pos]);
            intent.putExtra("PLAYLIST_FILTER_KEY", PLAYLIST_FILTER_VALUES[pos]);
            playlistSongLauncher.launch(intent);
        });
        popularRecyclerView.setAdapter(popularAdapter);
    }

    private void setupSearchRecyclerView() {
        searchRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        searchAdapter = new SongAdapter(this, searchResults);
        searchRecyclerView.setAdapter(searchAdapter);
        searchAdapter.setOnItemClickListener(this);
    }

    private void setupSearchFunction() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String q) { performSearch(q); return true; }
            @Override public boolean onQueryTextChange(String q) { performSearch(q); return true; }
        });
    }

    private void performSearch(String q) {
        searchResults.clear();
        if (q.isEmpty()) searchResults.addAll(allSongs);
        else {
            for (ModelClass s : allSongs)
                if (s.getSong_name().toLowerCase().contains(q.toLowerCase())) searchResults.add(s);
        }
        searchAdapter.notifyDataSetChanged();
    }

    private void setupToggleButton() {
        btnToggleList.setOnClickListener(v -> {
            if (searchRecyclerView.getVisibility() == View.VISIBLE) hideSearchList();
            else showSearchList();
        });
    }

    private void showSearchList() { searchRecyclerView.setVisibility(View.VISIBLE); performSearch(""); }
    private void hideSearchList() { searchRecyclerView.setVisibility(View.GONE); }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) mediaPlayer.release();
        handler.removeCallbacks(runnable);
    }
}