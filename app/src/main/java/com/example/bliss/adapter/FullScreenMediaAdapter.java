package com.example.bliss.adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.bliss.R;
import java.util.List;

public class FullScreenMediaAdapter extends RecyclerView.Adapter<FullScreenMediaAdapter.MediaViewHolder> {

    private Context context;
    private List<String> mediaUris;
    private List<String> mediaTypes; // "image" or "video"

    public FullScreenMediaAdapter(Context context, List<String> mediaUris, List<String> mediaTypes) {
        this.context = context;
        this.mediaUris = mediaUris;
        this.mediaTypes = mediaTypes;
    }

    @NonNull
    @Override
    public MediaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_full_screen_media, parent, false);
        return new MediaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MediaViewHolder holder, int position) {
        String uri = mediaUris.get(position);
        String type = mediaTypes.get(position);

        if ("video".equals(type)) {
            holder.ivPlayIcon.setVisibility(View.VISIBLE);
            // Load thumbnail for video
            Glide.with(context)
                    .load(uri)
                    .placeholder(android.R.drawable.ic_media_play)
                    .error(android.R.drawable.ic_media_play)
                    .into(holder.ivMedia);
            
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse(uri), "video/*");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                try {
                    context.startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(context, "No video player found", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            holder.ivPlayIcon.setVisibility(View.GONE);
            // Load image
            Glide.with(context)
                    .load(uri)
                    .into(holder.ivMedia);
            holder.itemView.setOnClickListener(null); // Or toggle controls
        }
    }

    @Override
    public int getItemCount() {
        return mediaUris.size();
    }

    static class MediaViewHolder extends RecyclerView.ViewHolder {
        ImageView ivMedia;
        ImageView ivPlayIcon;

        public MediaViewHolder(@NonNull View itemView) {
            super(itemView);
            ivMedia = itemView.findViewById(R.id.ivMedia);
            ivPlayIcon = itemView.findViewById(R.id.ivPlayIcon);
        }
    }
}
