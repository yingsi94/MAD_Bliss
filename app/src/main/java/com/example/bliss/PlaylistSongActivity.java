package com.example.bliss;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class PlaylistSongActivity extends AppCompatActivity implements SongAdapter.OnItemClickListener {

    private FirebaseFirestore db;
    private RecyclerView songRecyclerView;
    private TextView tvPlaylistTitle;
    private SongAdapter songAdapter;

    private final ArrayList<ModelClass> playlistSongs = new ArrayList<>();

    private String receivedPlaylistTitle;
    private String receivedMoodTag; // 保留，但用于流行歌单筛选时不再使用
    private String receivedFilterKey; // ★★★ 新增：用于接收 MusicSuggestionActivity 传递的新筛选值 ★★★

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_happiness_playlist);

        db = FirebaseFirestore.getInstance();
        tvPlaylistTitle = findViewById(R.id.tv_playlist_title);
        songRecyclerView = findViewById(R.id.playlistRecyclerView);
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        if (getIntent().getExtras() != null) {
            receivedPlaylistTitle = getIntent().getStringExtra("PLAYLIST_TITLE");
            receivedMoodTag = getIntent().getStringExtra("MOOD_TAG"); // 接收 Mood 标签
            receivedFilterKey = getIntent().getStringExtra("PLAYLIST_FILTER_KEY"); // ★★★ 接收新的筛选值 ★★★
        } else {
            receivedPlaylistTitle = "Unknown Playlist";
            receivedMoodTag = "all";
            receivedFilterKey = null;
        }

        tvPlaylistTitle.setText(receivedPlaylistTitle);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        songRecyclerView.setLayoutManager(layoutManager);
        songAdapter = new SongAdapter(this, playlistSongs);
        songRecyclerView.setAdapter(songAdapter);
        songAdapter.setOnItemClickListener(this);

        // ★★★ 关键修改：调用新的加载方法，使用 receivedFilterKey 进行筛选 ★★★
        loadPlaylistSongs(receivedFilterKey);
    }

    /**
     * 根据新的筛选 Key (例如 Firestore 中的 'playlist' 字段) 加载歌曲。
     * @param filterValue 用于查询 Firestore 'playlist' 字段的值 (e.g., "popular_happiness")
     */
    private void loadPlaylistSongs(String filterValue) {
        playlistSongs.clear();

        if (filterValue == null || filterValue.isEmpty()) {
            Toast.makeText(this, "Playlist filter is missing.", Toast.LENGTH_LONG).show();
            if (songAdapter != null) songAdapter.updateList(playlistSongs);
            return;
        }

        Toast.makeText(this, "Loading songs for " + receivedPlaylistTitle + "...", Toast.LENGTH_SHORT).show();

        db.collection("songs")
                // ★★★ 关键：使用 whereEqualTo("playlist", filterValue) 进行服务器端筛选 ★★★
                .whereEqualTo("playlist", filterValue)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                // 歌曲解析和 ModelClass 创建逻辑 (与原代码类似)
                                String songName = document.getString("songName");
                                String singer = document.getString("singer");
                                String duration = document.getString("duration");
                                String audioUrl = document.getString("audioUrl");
                                String imgFileName = document.getString("imgFileName");
                                String firestoreMood = document.getString("mood"); // 保留 mood 字段读取

                                ModelClass song = new ModelClass();
                                song.setSong_name(songName);
                                song.setSinger(singer);
                                song.setAudioUrl(audioUrl);
                                song.setDuration(duration != null ? duration : "N/A");

                                int imageResId = getResourceIdForImage(imgFileName);
                                song.setImg(imageResId);
                                song.setMood(firestoreMood);

                                playlistSongs.add(song);
                            } catch (Exception e) {
                                Log.e("PlaylistSong", "Error parsing song document: " + document.getId(), e);
                            }
                        }

                        if (playlistSongs.isEmpty()) {
                            Toast.makeText(this, "No songs found for this playlist.", Toast.LENGTH_LONG).show();
                        }
                        if (songAdapter != null) {
                            // 假设 songAdapter 有 updateList 方法
                            // 如果没有，请使用 songAdapter.notifyDataSetChanged();
                            songAdapter.updateList(playlistSongs);
                        }
                        Log.d("PlaylistSong", "Loaded " + playlistSongs.size() + " songs using filter: " + filterValue);

                    } else {
                        Log.w("PlaylistSong", "Error getting documents.", task.getException());
                        Toast.makeText(this, "Failed to load songs. Check Firestore connection.", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private int getResourceIdForImage(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return R.drawable.img_default;
        }

        int resourceId = getResources().getIdentifier(
                fileName,
                "drawable",
                getPackageName()
        );

        if (resourceId == 0) {
            return R.drawable.img_default;
        }
        return resourceId;
    }

    @Override
    public void onItemClick(int position, ModelClass song) {
        // 此处逻辑保持不变：将选中的歌曲数据返回给 MusicSuggestionActivity

        Intent resultIntent = new Intent();
        resultIntent.putExtra("SONG_NAME", song.getSong_name());
        resultIntent.putExtra("SINGER", song.getSinger());
        resultIntent.putExtra("DURATION", song.getDuration());
        resultIntent.putExtra("AUDIO_URL", song.getAudioUrl());
        resultIntent.putExtra("IMG_RES_ID", song.getImg());

        setResult(Activity.RESULT_OK, resultIntent);
        finish();

        Toast.makeText(this, "Selected: " + song.getSong_name(), Toast.LENGTH_SHORT).show();
    }
}