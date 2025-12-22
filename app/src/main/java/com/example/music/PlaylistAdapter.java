package com.example.music;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bliss.R;

import java.util.ArrayList;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.MyHolder> {

    public interface OnPlaylistClickListener {
        void onPlaylistClick(int position, PlaylistModel playlist);
    }

    private final ArrayList<PlaylistModel> playlistList;
    private final OnPlaylistClickListener listener;

    public PlaylistAdapter(Context context, ArrayList<PlaylistModel> playlistList, OnPlaylistClickListener listener) {
        this.playlistList = playlistList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 假设您的布局文件名为 item_popular_playlist.xml
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_popular_playlist, parent, false);
        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {
        PlaylistModel playlist = playlistList.get(position);

        holder.playlistImage.setImageResource(playlist.getPlaylistImage());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                int currentPosition = holder.getBindingAdapterPosition();
                if (currentPosition != RecyclerView.NO_POSITION) {
                    listener.onPlaylistClick(currentPosition, playlistList.get(currentPosition));
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return playlistList.size();
    }

    public static class MyHolder extends RecyclerView.ViewHolder {
        ImageView playlistImage;

        public MyHolder(@NonNull View itemView) {
            super(itemView);
            // 确保 ID 匹配 R.id.img_playlist
            playlistImage = itemView.findViewById(R.id.img_playlist);
        }
    }
}