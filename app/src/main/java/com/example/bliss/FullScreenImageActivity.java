package com.example.bliss;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.widget.ImageButton;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;

public class FullScreenImageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen_image);

        ImageView ivFullScreen = findViewById(R.id.ivFullScreen);
        ImageButton btnClose = findViewById(R.id.btnClose);

        String imageSource = getIntent().getStringExtra("image_source");

        if (imageSource != null) {
            if (imageSource.startsWith("http")) {
                // Cloudinary or other web URL
                Glide.with(this)
                    .load(imageSource)
                    .into(ivFullScreen);
            } else if (imageSource.startsWith("content") || imageSource.startsWith("file")) {
                // Local URI
                Glide.with(this).load(imageSource).into(ivFullScreen);
            } else {
                // Base64 (fallback for old entries)
                try {
                    byte[] imageBytes = Base64.decode(imageSource, Base64.DEFAULT);
                    Glide.with(this)
                            .asBitmap()
                            .load(imageBytes)
                            .into(ivFullScreen);
                } catch (IllegalArgumentException e) {
                    Glide.with(this).load(imageSource).into(ivFullScreen);
                }
            }
        }

        btnClose.setOnClickListener(v -> finish());
    }
}