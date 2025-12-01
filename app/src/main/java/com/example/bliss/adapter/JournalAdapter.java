package com.example.bliss.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.bliss.R;
import com.example.bliss.model.JournalEntry;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class JournalAdapter extends RecyclerView.Adapter<JournalAdapter.JournalViewHolder> {

    private List<JournalEntry> journalList;
    private Context context;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(JournalEntry entry);
    }

    public JournalAdapter(Context context, List<JournalEntry> journalList, OnItemClickListener listener) {
        this.context = context;
        this.journalList = journalList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public JournalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_journal_entry, parent, false);
        return new JournalViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull JournalViewHolder holder, int position) {
        JournalEntry entry = journalList.get(position);
        holder.bind(entry, listener);
    }

    @Override
    public int getItemCount() {
        return journalList.size();
    }

    public class JournalViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvTitle, tvContentSnippet;
        ImageView ivMoodIcon, ivJournalImage;

        public JournalViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvContentSnippet = itemView.findViewById(R.id.tvContentSnippet);
            ivMoodIcon = itemView.findViewById(R.id.ivMoodIcon);
            ivJournalImage = itemView.findViewById(R.id.ivJournalImage);
        }

        public void bind(final JournalEntry entry, final OnItemClickListener listener) {
            tvTitle.setText(entry.getTitle());
            tvContentSnippet.setText(entry.getContent());
            
            if (entry.getDate() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                tvDate.setText(sdf.format(entry.getDate().toDate()));
            }

            if (entry.getImageUri() != null && !entry.getImageUri().isEmpty()) {
                ivJournalImage.setVisibility(View.VISIBLE);
                Glide.with(context).load(entry.getImageUri()).into(ivJournalImage);
            } else {
                ivJournalImage.setVisibility(View.GONE);
            }

            // Simple mood mapping (you can expand this)
            if (entry.getMood() != null) {
                if (entry.getMood().toLowerCase().contains("happy")) {
                    ivMoodIcon.setImageResource(android.R.drawable.btn_star_big_on); // Placeholder
                } else if (entry.getMood().toLowerCase().contains("sad")) {
                    ivMoodIcon.setImageResource(android.R.drawable.ic_delete); // Placeholder
                }
            }

            itemView.setOnClickListener(v -> listener.onItemClick(entry));
        }
    }
}
