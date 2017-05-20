package com.akikoz.securechat.adapter;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.akikoz.securechat.ChatDetailActivity;
import com.akikoz.securechat.R;
import com.akikoz.securechat.model.Chat;
import com.akikoz.securechat.model.Msg;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

    private Context mContext;
    private List<Chat> mChatList;

    static class ViewHolder extends RecyclerView.ViewHolder {

        View view;
        TextView person;
        TextView msg;
        TextView unreadCount;

        ViewHolder(View view) {
            super(view);
            this.view = view;
            person = (TextView) view.findViewById(R.id.person);
            msg = (TextView) view.findViewById(R.id.msg);
            unreadCount = (TextView) view.findViewById(R.id.unread_count);
        }

    }

    public ChatAdapter(List<Chat> chatList) {
        mChatList = chatList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (mContext == null) {
            mContext = parent.getContext();
        }
        View view = LayoutInflater.from(mContext).inflate(R.layout.chat_item, parent, false);
        final ViewHolder holder = new ViewHolder(view);
        holder.view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Chat chat = mChatList.get(holder.getAdapterPosition());
                chat.unreadCount = 0;
                Intent intent = new Intent(mContext, ChatDetailActivity.class);
                intent.putExtra("chat_data", chat);
                mContext.startActivity(intent);
            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Chat chat = mChatList.get(position);
        List<Msg> msgList = chat.msgList;
        holder.person.setText(chat.getPerson());
        holder.msg.setText(msgList.get(msgList.size() - 1).getContent());
        holder.unreadCount.setVisibility(chat.unreadCount == 0 ? View.GONE : View.VISIBLE);
        holder.unreadCount.setText(String.valueOf(chat.unreadCount));
    }

    @Override
    public int getItemCount() {
        return mChatList.size();
    }

}
