package com.example.bliss;

import android.os.Bundle;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.example.bliss.adapter.FullScreenMediaAdapter;
import java.util.ArrayList;

public class FullScreenImageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen_image);

        ViewPager2 viewPager = findViewById(R.id.viewPager);
        ImageButton btnClose = findViewById(R.id.btnClose);

        ArrayList<String> mediaUris = getIntent().getStringArrayListExtra("media_uris");
        ArrayList<String> mediaTypes = getIntent().getStringArrayListExtra("media_types");
        int startIndex = getIntent().getIntExtra("start_index", 0);

        // Backward compatibility for single image intent
        if (mediaUris == null) {
            String imageSource = getIntent().getStringExtra("image_source");
            if (imageSource != null) {
                mediaUris = new ArrayList<>();
                mediaUris.add(imageSource);
                mediaTypes = new ArrayList<>();
                mediaTypes.add("image");
            }
        }

        if (mediaUris != null && mediaTypes != null) {
            FullScreenMediaAdapter adapter = new FullScreenMediaAdapter(this, mediaUris, mediaTypes);
            viewPager.setAdapter(adapter);
            viewPager.setCurrentItem(startIndex, false);
        }

        btnClose.setOnClickListener(v -> finish());
    }
}