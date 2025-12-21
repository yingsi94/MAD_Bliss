package com.example.bliss.ai;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.example.bliss.BuildConfig;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class GeminiHelper {
    // API Key is now loaded from BuildConfig (which gets it from local.properties)
    private static final String API_KEY = BuildConfig.GOOGLE_API_KEY;
    private final GenerativeModelFutures model;
    private final Executor executor = Executors.newSingleThreadExecutor();

    public GeminiHelper() {
        if (API_KEY == null || API_KEY.isEmpty() || API_KEY.equals("null")) {
            android.util.Log.e("GeminiHelper", "API Key is MISSING! Check local.properties and build.");
        } else {
            android.util.Log.d("GeminiHelper", "API Key loaded. Length: " + API_KEY.length());
        }

        // Using the requested model
        GenerativeModel gm = new GenerativeModel("gemini-2.5-flash", API_KEY);
        this.model = GenerativeModelFutures.from(gm);
    }

    public void analyzeJournalEntry(String title, String content, AnalysisCallback callback) {
        String prompt = "Analyze the following journal entry. " +
                "Identify the mood (e.g., Happy, Sad, Anxious, Neutral) and suggest a helpful activity or advice. " +
                "Format the response as: Mood: [Mood]\nSuggestion: [Suggestion]\n\n" +
                "Title: " + title + "\n" +
                "Content: " + content;

        android.util.Log.d("GeminiHelper", "Sending prompt to Gemini: " + prompt);

        Content userContent = new Content.Builder()
                .addText(prompt)
                .build();

        ListenableFuture<GenerateContentResponse> response = model.generateContent(userContent);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String text = result.getText();
                callback.onAnalysisSuccess(text);
            }

            @Override
            public void onFailure(Throwable t) {
                callback.onAnalysisFailure(t);
            }
        }, executor);
    }

    public interface AnalysisCallback {
        void onAnalysisSuccess(String result);
        void onAnalysisFailure(Throwable t);
    }
}
