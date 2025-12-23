package com.example.chatbox;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bliss.R;
import com.example.chatbox.adapters.MessageAdapter;
import com.example.chatbox.models.Message;
import com.example.chatbox.models.MyResponse;
import com.example.chatbox.services.ApiService;
import com.example.chatbox.services.RetrofitClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AIChatbotFragment extends Fragment {
    private RecyclerView recyclerChat;
    private MessageAdapter messageAdapter;
    private List<Message> messageList;
    private EditText editMessage;
    private ImageButton btnSend;
    private LinearLayout welcomeSection;

    public AIChatbotFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 直接返回布局 View
        return inflater.inflate(R.layout.fragment_ai_chatbot, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 初始化视图组件
        recyclerChat = view.findViewById(R.id.recycler_gchat);
        editMessage = view.findViewById(R.id.layout_input);
        btnSend = view.findViewById(R.id.sendChatBtn);
        welcomeSection = view.findViewById(R.id.welcomeSection);

        // 设置 RecyclerView
        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(getContext(), messageList);
        recyclerChat.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerChat.setAdapter(messageAdapter);

        ApiService apiService = RetrofitClient.getApiService();

        btnSend.setOnClickListener(v -> {
            String text = editMessage.getText().toString().trim();
            if (text.isEmpty()) return;

            // 隐藏欢迎图示
            welcomeSection.setVisibility(View.GONE);

            // 添加用户消息
            addMessage(new Message(text, "Me", System.currentTimeMillis()));
            editMessage.setText("");

            // 调用 API
            apiService.getMessage(text).enqueue(new Callback<MyResponse>() {
                @Override
                public void onResponse(Call<MyResponse> call, Response<MyResponse> response) {
                    // Fragment 必须检查 isAdded 防止异步回调时 Fragment 已销毁
                    if (isAdded() && response.isSuccessful() && response.body() != null) {
                        addMessage(new Message(response.body().getMessage(), "BlissMate", System.currentTimeMillis()));
                    }
                }

                @Override
                public void onFailure(Call<MyResponse> call, Throwable t) {
                    if (isAdded()) {
                        addMessage(new Message("Connection Error", "Agent", System.currentTimeMillis()));
                    }
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