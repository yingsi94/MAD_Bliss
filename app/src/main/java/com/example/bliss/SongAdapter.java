package com.example.bliss;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.MyHolder> {

    private ArrayList<ModelClass> arrayList;
    private OnItemClickListener onItemClickListener; // 点击事件监听器

    public SongAdapter(Context context, ArrayList<ModelClass> arrayList) {
        this.arrayList = arrayList;
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song_layout, parent, false);
        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {
        ModelClass model = arrayList.get(position);

        holder.song_name.setText(model.getSong_name());
        holder.singer.setText(model.getSinger());
        holder.duration.setText(model.getDuration());

        if (model.getImg() != 0) {
            holder.img.setImageResource(model.getImg());
        }

        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(position, model);
            }
        });
    }

    @Override
    public int getItemCount() {
        return arrayList == null ? 0 : arrayList.size();
    }

    public static class MyHolder extends RecyclerView.ViewHolder {
        TextView song_name, singer, duration;
        ImageView img;

        public MyHolder(@NonNull View itemView) {
            super(itemView);
            song_name = itemView.findViewById(R.id.txt);
            singer = itemView.findViewById(R.id.txt2);
            duration = itemView.findViewById(R.id.txt_duration);
            img = itemView.findViewById(R.id.img);
        }
    }

    public void updateList(ArrayList<ModelClass> newList) {
        if (newList != null) {
            this.arrayList = newList;
            notifyDataSetChanged();
        }
    }

    public interface OnItemClickListener {
        void onItemClick(int position, ModelClass song);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }
}