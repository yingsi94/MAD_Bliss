package com.example.chatbox.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bliss.R;
import com.example.chatbox.models.Message;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_ME = 1;
    private static final int TYPE_AI = 2;

    private Context context;
    private List<Message> messageList;

    public MessageAdapter(Context context, List<Message> messageList) {
        this.context = context;
        this.messageList = messageList;
    }

    @Override
    public int getItemViewType(int position) {
        // If the sender is "Me", show the right bubble
        return messageList.get(position).getSender().equalsIgnoreCase("Me") ? TYPE_ME : TYPE_AI;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_ME) {
            View v = LayoutInflater.from(context).inflate(R.layout.item_chat_me, parent, false);
            return new SentViewHolder(v);
        } else {
            View v = LayoutInflater.from(context).inflate(R.layout.item_chat_ai, parent, false);
            return new ReceivedViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messageList.get(position);
        if (holder instanceof SentViewHolder) {
            ((SentViewHolder) holder).bind(message);
        } else {
            ((ReceivedViewHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() { return messageList.size(); }

    // ViewHolder for messages sent by the user
    static class SentViewHolder extends RecyclerView.ViewHolder {
        TextView msg, time;
        SentViewHolder(View v) {
            super(v);
            msg = v.findViewById(R.id.text_chat_message_me);
            time = v.findViewById(R.id.text_chat_timestamp_me);
        }
        void bind(Message m) {
            msg.setText(m.getMessage());
            time.setText(m.getFormattedTime());
        }
    }

    // ViewHolder for messages received from the AI
    static class ReceivedViewHolder extends RecyclerView.ViewHolder {
        TextView msg, time, user;
        ReceivedViewHolder(View v) {
            super(v);
            msg = v.findViewById(R.id.text_chat_message_ai);
            time = v.findViewById(R.id.text_chat_timestamp_ai);
            user = v.findViewById(R.id.text_chat_user_ai);
        }
        void bind(Message m) {
            msg.setText(m.getMessage());
            time.setText(m.getFormattedTime());
            user.setText(m.getSender());
        }
    }
}