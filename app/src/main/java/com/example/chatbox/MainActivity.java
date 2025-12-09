package com.example.chatbox;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatbox.adapters.MessageAdapter;
import com.example.chatbox.models.Message;
import com.example.chatbox.models.MyResponse;
import com.example.chatbox.services.ApiService;
import com.example.chatbox.services.RetrofitClient;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerChat;
    private MessageAdapter messageAdapter;
    private List<Message> messageList;
    private EditText editMessage;
    private ImageButton btnSend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Initialize Views
        recyclerChat = findViewById(R.id.recycler_gchat);
        editMessage = findViewById(R.id.layout_input);
        btnSend = findViewById(R.id.sendChatBtn);

        // 2. Initialize Data and Adapter
        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(this, messageList);

        // 3. Setup RecyclerView
        // LinearLayoutManager aligns items in a vertical list
        recyclerChat.setLayoutManager(new LinearLayoutManager(this));
        recyclerChat.setAdapter(messageAdapter);
        ApiService apiService = RetrofitClient.getApiService();
        // 4. Handle Send Button
        btnSend.setOnClickListener(v -> {
            ImageView imgLogo = findViewById(R.id.imgLogo);
            TextView tvGreeting = findViewById(R.id.tvGreeting);
            LocalTime currentTime = LocalTime.now();
            String text = editMessage.getText().toString();
            imgLogo.setVisibility(View.GONE);
            tvGreeting.setVisibility(View.GONE);
            if (!text.isEmpty()) {
                // Create new message object
                Message userMessage = new Message(text, "Me", currentTime);
                messageList.add(userMessage);
                messageAdapter.notifyItemInserted(messageList.size() - 1);
                recyclerChat.scrollToPosition(messageList.size() - 1);
                editMessage.setText("");
                // Call API asynchronously
                apiService.getMessage(text).enqueue(new retrofit2.Callback<MyResponse>() {
                    @Override
                    public void onResponse(retrofit2.Call<MyResponse> call, retrofit2.Response<MyResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            String result = response.body().getMessage();
                            Message agentMessage = new Message(result, "Agent", LocalTime.now());
                            messageList.add(agentMessage);
                            messageAdapter.notifyItemInserted(messageList.size() - 1);
                            recyclerChat.scrollToPosition(messageList.size() - 1);
                        }
                    }
                    @Override
                    public void onFailure(retrofit2.Call<MyResponse> call, Throwable t) {
                        Message agentMessage = new Message("Error: " + t.getMessage(), "Agent", LocalTime.now());
                        messageList.add(agentMessage);
                        messageAdapter.notifyItemInserted(messageList.size() - 1);
                        recyclerChat.scrollToPosition(messageList.size() - 1);
                    }
                });
            }
        });
    }
}