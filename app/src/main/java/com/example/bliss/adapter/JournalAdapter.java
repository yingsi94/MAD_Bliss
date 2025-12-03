package com.example.bliss.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
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

    private int getMoodIconResource(String mood) {
        if (mood == null) return R.drawable.neutral;
        String lowerMood = mood.toLowerCase();
        if (lowerMood.contains("happy")) {
            return R.drawable.happy;
        } else if (lowerMood.contains("sad")) {
            return R.drawable.sad;
        } else if (lowerMood.contains("angry")) {
            return R.drawable.angry;
        } else if (lowerMood.contains("anxious")) {
            return R.drawable.anxious;
        } else {
            return R.drawable.neutral;
        }
    }

    private int getMoodColor(String mood) {
        if (mood == null) return ContextCompat.getColor(context, R.color.mood_neutral);
        String lowerMood = mood.toLowerCase();
        if (lowerMood.contains("happy")) {
            return ContextCompat.getColor(context, R.color.mood_happy);
        } else if (lowerMood.contains("sad")) {
            return ContextCompat.getColor(context, R.color.mood_sad);
        } else if (lowerMood.contains("angry")) {
            return ContextCompat.getColor(context, R.color.mood_angry);
        } else if (lowerMood.contains("anxious")) {
            return ContextCompat.getColor(context, R.color.mood_anxious);
        } else {
            return ContextCompat.getColor(context, R.color.mood_neutral);
        }
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
        TextView tvDate, tvTitle, tvContentSnippet, tvMoreImagesCount;
        ImageView ivMoodIcon, ivJournalImage, ivJournalImage2;
        View mediaContainer, frameImage2;

        public JournalViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvContentSnippet = itemView.findViewById(R.id.tvContentSnippet);
            ivMoodIcon = itemView.findViewById(R.id.ivMoodIcon);
            ivJournalImage = itemView.findViewById(R.id.ivJournalImage);
            ivJournalImage2 = itemView.findViewById(R.id.ivJournalImage2);
            tvMoreImagesCount = itemView.findViewById(R.id.tvMoreImagesCount);
            mediaContainer = itemView.findViewById(R.id.mediaContainer);
            frameImage2 = itemView.findViewById(R.id.frameImage2);
        }

        public void bind(final JournalEntry entry, final OnItemClickListener listener) {
            tvTitle.setText(entry.getTitle());
            tvContentSnippet.setText(entry.getContent());
            
            if (entry.getDate() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                tvDate.setText(sdf.format(entry.getDate().toDate()));
            }

            // Reset visibility
            mediaContainer.setVisibility(View.GONE);
            ivJournalImage.setVisibility(View.GONE);
            frameImage2.setVisibility(View.GONE);
            tvMoreImagesCount.setVisibility(View.GONE);



            // Check for multiple images
            if (entry.getImageUris() != null && !entry.getImageUris().isEmpty()) {
                mediaContainer.setVisibility(View.VISIBLE);
                ivJournalImage.setVisibility(View.VISIBLE);
                loadImage(entry.getImageUris().get(0), ivJournalImage);
                
                if (entry.getImageUris().size() > 1) {
                    frameImage2.setVisibility(View.VISIBLE);
                    loadImage(entry.getImageUris().get(1), ivJournalImage2);
                    
                    if (entry.getImageUris().size() > 2) {
                        tvMoreImagesCount.setVisibility(View.VISIBLE);
                        tvMoreImagesCount.setText("+" + (entry.getImageUris().size() - 2));
                    }
                }
            } 
            // Check for single image (backward compatibility)
            else if (entry.getImageUri() != null && !entry.getImageUri().isEmpty()) {
                mediaContainer.setVisibility(View.VISIBLE);
                ivJournalImage.setVisibility(View.VISIBLE);
                loadImage(entry.getImageUri(), ivJournalImage);
            }
            // Check for multiple videos
            else if (entry.getVideoUris() != null && !entry.getVideoUris().isEmpty()) {
                mediaContainer.setVisibility(View.VISIBLE);
                ivJournalImage.setVisibility(View.VISIBLE);
                
                // Try to load thumbnail if available
                String thumb1 = (entry.getVideoThumbnails() != null && entry.getVideoThumbnails().size() > 0) 
                        ? entry.getVideoThumbnails().get(0) : null;
                
                if (thumb1 != null && !thumb1.isEmpty()) {
                    loadImage(thumb1, ivJournalImage);
                } else {
                    // Fallback to URI loading (might fail on emulator)
                    Glide.with(context)
                         .load(entry.getVideoUris().get(0))
                         .placeholder(android.R.drawable.ic_media_play)
                         .error(android.R.drawable.ic_media_play)
                         .into(ivJournalImage);
                }

                if (entry.getVideoUris().size() > 1) {
                    frameImage2.setVisibility(View.VISIBLE);
                    
                    String thumb2 = (entry.getVideoThumbnails() != null && entry.getVideoThumbnails().size() > 1) 
                            ? entry.getVideoThumbnails().get(1) : null;

                    if (thumb2 != null && !thumb2.isEmpty()) {
                        loadImage(thumb2, ivJournalImage2);
                    } else {
                        Glide.with(context)
                             .load(entry.getVideoUris().get(1))
                             .placeholder(android.R.drawable.ic_media_play)
                             .error(android.R.drawable.ic_media_play)
                             .into(ivJournalImage2);
                    }

                    if (entry.getVideoUris().size() > 2) {
                        tvMoreImagesCount.setVisibility(View.VISIBLE);
                        tvMoreImagesCount.setText("+" + (entry.getVideoUris().size() - 2));
                    }
                }
            }
            // Check for single video (backward compatibility)
            else if (entry.getVideoUri() != null && !entry.getVideoUri().isEmpty()) {
                mediaContainer.setVisibility(View.VISIBLE);
                ivJournalImage.setVisibility(View.VISIBLE);
                
                // Try to load thumbnail if available
                String thumb1 = (entry.getVideoThumbnails() != null && entry.getVideoThumbnails().size() > 0) 
                        ? entry.getVideoThumbnails().get(0) : null;
                
                if (thumb1 != null && !thumb1.isEmpty()) {
                    loadImage(thumb1, ivJournalImage);
                } else {
                    Glide.with(context)
                         .load(entry.getVideoUri())
                         .placeholder(android.R.drawable.ic_media_play)
                         .error(android.R.drawable.ic_media_play)
                         .into(ivJournalImage);
                }
            }

            // Set mood icon and color based on detected mood
            int moodIconRes = getMoodIconResource(entry.getMood());
            ivMoodIcon.setImageResource(moodIconRes);
            ivMoodIcon.setBackgroundTintList(ColorStateList.valueOf(getMoodColor(entry.getMood())));
            ivMoodIcon.clearColorFilter(); // Ensure emoji itself isn't tinted

            itemView.setOnClickListener(v -> listener.onItemClick(entry));
        }

        private void loadImage(String imageSource, ImageView targetView) {
            if (imageSource == null) return;
            
            // Cloudinary URLs start with "https://res.cloudinary.com" or just "http"
            if (imageSource.startsWith("http")) {
                // Cloudinary or other web URL
                Glide.with(context)
                    .load(imageSource)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .into(targetView);
            } else if (imageSource.startsWith("content") || imageSource.startsWith("file")) {
                // Local URI loading
                Glide.with(context)
                    .load(imageSource)
                    .into(targetView);
            } else {
                // Check if it's Base64 (fallback for old entries)
                try {
                    byte[] imageBytes = Base64.decode(imageSource, Base64.DEFAULT);
                    Glide.with(context)
                        .asBitmap()
                        .load(imageBytes)
                        .into(targetView);
                } catch (IllegalArgumentException e) {
                    // Fallback if decoding fails
                    Glide.with(context)
                        .load(imageSource)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_gallery)
                        .into(targetView);
                }
            }
        }
    }
}
