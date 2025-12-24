package com.example.bliss;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import com.cloudinary.Cloudinary;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CloudinaryHelper {
    private static final String TAG = "CloudinaryHelper";
    private Context context;
    private Cloudinary cloudinary;

    public CloudinaryHelper(Context context) {
        this.context = context;
        initializeCloudinary();
    }

    private void initializeCloudinary() {
        try {
            // Check if already initialized to avoid crash
            try {
                MediaManager.get();
                Log.d(TAG, "Cloudinary already initialized");
                return; 
            } catch (IllegalStateException e) {
                // Not initialized yet, proceed
            }

            // Configure Cloudinary
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", BuildConfig.CLOUDINARY_CLOUD_NAME);
            config.put("api_key", BuildConfig.CLOUDINARY_API_KEY);
            config.put("api_secret", BuildConfig.CLOUDINARY_API_SECRET);

            MediaManager.init(context, config);
            cloudinary = new Cloudinary(config);

            Log.d(TAG, "Cloudinary initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Cloudinary", e);
        }
    }

    public interface UploadListener {
        void onUploadSuccess(String publicUrl);
        void onUploadFailure(String error);
        void onUploadProgress(int progress);
    }

    public void uploadImage(Uri imageUri, UploadListener listener) {
        try {
            Map<String, Object> options = new HashMap<>();
            options.put("resource_type", "image");
            options.put("folder", "bliss_journal"); // Organize uploads in folder
            options.put("quality", "auto:good"); // Automatic quality optimization
            options.put("format", "jpg"); // Convert to JPG for smaller size

            MediaManager.get().upload(imageUri)
                .options(options)
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {
                        Log.d(TAG, "Upload started: " + requestId);
                        listener.onUploadProgress(0);
                    }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {
                        int progress = (int) ((bytes * 100) / totalBytes);
                        listener.onUploadProgress(progress);
                    }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String publicUrl = (String) resultData.get("secure_url");
                        Log.d(TAG, "Upload successful: " + publicUrl);
                        listener.onUploadSuccess(publicUrl);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Log.e(TAG, "Upload failed: " + error.getDescription());
                        listener.onUploadFailure(error.getDescription());
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {
                        Log.w(TAG, "Upload rescheduled: " + error.getDescription());
                    }
                })
                .dispatch();

        } catch (Exception e) {
            Log.e(TAG, "Error starting upload", e);
            listener.onUploadFailure("Failed to start upload: " + e.getMessage());
        }
    }

    public void uploadVideo(Uri videoUri, UploadListener listener) {
        try {
            Map<String, Object> options = new HashMap<>();
            options.put("resource_type", "video");
            options.put("folder", "bliss_journal");
            options.put("quality", "auto");
            options.put("format", "mp4"); // Force MP4 for Android compatibility

            MediaManager.get().upload(videoUri)
                .options(options)
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {
                        Log.d(TAG, "Video upload started: " + requestId);
                        listener.onUploadProgress(0);
                    }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {
                        int progress = (int) ((bytes * 100) / totalBytes);
                        listener.onUploadProgress(progress);
                    }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String publicUrl = (String) resultData.get("secure_url");
                        Log.d(TAG, "Video upload successful: " + publicUrl);
                        listener.onUploadSuccess(publicUrl);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Log.e(TAG, "Video upload failed: " + error.getDescription());
                        listener.onUploadFailure(error.getDescription());
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {
                        Log.w(TAG, "Video upload rescheduled: " + error.getDescription());
                    }
                })
                .dispatch();

        } catch (Exception e) {
            Log.e(TAG, "Error starting video upload", e);
            listener.onUploadFailure("Failed to start video upload: " + e.getMessage());
        }
    }
}