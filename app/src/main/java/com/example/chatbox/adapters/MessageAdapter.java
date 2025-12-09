package com.example.chatbox.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import com.example.chatbox.R;
import com.example.chatbox.models.Message;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private Context context;
    private List<Message> messageList;

    // Constructor: Pass in the list of messages
    public MessageAdapter(Context context, List<Message> messageList) {
        this.context = context;
        this.messageList = messageList;
    }

    // 1. Create the view (inflate item_message.xml)
    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    // 2. Bind the data (take data from list, put it on screen)
    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messageList.get(position);

        holder.textMessage.setText(message.getMessage());
        holder.textSender.setText(message.getSender());
        // Simple timestamp for demo. In real app, format the long 'createdAt' to a date string.
        holder.textTimestamp.setText(String.valueOf(message.getCreatedAt()));
    }

    // 3. How many items do we have?
    @Override
    public int getItemCount() {
        return messageList.size();
    }

    // Inner Class: Defines the View Components
    public class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView textMessage, textSender, textTimestamp;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            // Link the Java variables to the XML IDs here
            textMessage = itemView.findViewById(R.id.text_gchat_message);
            textSender = itemView.findViewById(R.id.text_gchat_user);
            textTimestamp = itemView.findViewById(R.id.text_gchat_timestamp);
        }
    }
}