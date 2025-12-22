package com.example.chatbox;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bliss.R;
import com.example.chatbox.adapters.MessageAdapter;
import com.example.chatbox.models.Message;
import com.example.chatbox.models.MyResponse;
import com.example.chatbox.services.ApiService;
import com.example.chatbox.services.RetrofitClient;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AI_Chatbot extends AppCompatActivity {
    private RecyclerView recyclerChat;
    private MessageAdapter messageAdapter;
    private List<Message> messageList;
    private EditText editMessage;
    private ImageButton btnSend;
    private LinearLayout welcomeSection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chatbot);

        recyclerChat = findViewById(R.id.recycler_gchat);
        editMessage = findViewById(R.id.layout_input);
        btnSend = findViewById(R.id.sendChatBtn);
        welcomeSection = findViewById(R.id.welcomeSection);

        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(this, messageList);
        recyclerChat.setLayoutManager(new LinearLayoutManager(this));
        recyclerChat.setAdapter(messageAdapter);

        ApiService apiService = RetrofitClient.getApiService();

        btnSend.setOnClickListener(v -> {
            String text = editMessage.getText().toString().trim();
            if (text.isEmpty()) return;

            // Hide the logo once chatting starts
            welcomeSection.setVisibility(View.GONE);

            // 1. Add User Message
            addMessage(new Message(text, "Me", System.currentTimeMillis()));
            editMessage.setText("");

            // 2. Call API for AI Response
            apiService.getMessage(text).enqueue(new Callback<MyResponse>() {
                @Override
                public void onResponse(Call<MyResponse> call, Response<MyResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        addMessage(new Message(response.body().getMessage(), "BlissMate", System.currentTimeMillis()));
                    }
                }
                @Override
                public void onFailure(Call<MyResponse> call, Throwable t) {
                    addMessage(new Message("Connection Error", "Agent", System.currentTimeMillis()));
                }
            });
        });
    }

    private void addMessage(Message msg) {
        messageList.add(msg);
        messageAdapter.notifyItemInserted(messageList.size() - 1);
        recyclerChat.scrollToPosition(messageList.size() - 1);
    }
}