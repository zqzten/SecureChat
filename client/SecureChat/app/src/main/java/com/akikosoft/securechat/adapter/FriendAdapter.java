package com.akikosoft.securechat.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.afollestad.sectionedrecyclerview.SectionedRecyclerViewAdapter;
import com.afollestad.sectionedrecyclerview.SectionedViewHolder;
import com.akikosoft.securechat.ChatDetailActivity;
import com.akikosoft.securechat.R;
import com.akikosoft.securechat.SecureChatApplication;
import com.akikosoft.securechat.model.Chat;
import com.akikosoft.securechat.model.Friend;
import io.socket.client.Ack;

import java.util.List;

public class FriendAdapter extends SectionedRecyclerViewAdapter<FriendAdapter.ViewHolder> {

    private SecureChatApplication mApp;
    private Context mContext;
    private List<Friend> mFriendList;
    private List<String> mPendingFriendList;

    static class ViewHolder extends SectionedViewHolder {

        View view;
        TextView title;
        TextView email;
        Button accept;
        Button deny;

        ViewHolder(View view) {
            super(view);
            this.view = view;
            title = (TextView) view.findViewById(R.id.title);
            email = (TextView) view.findViewById(R.id.email);
            accept = (Button) view.findViewById(R.id.accept);
            deny = (Button) view.findViewById(R.id.deny);
        }

    }

    public FriendAdapter(SecureChatApplication app) {
        mApp = app;
        mFriendList = app.friendList;
        mPendingFriendList = app.pendingFriendList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (mContext == null) {
            mContext = parent.getContext();
        }
        final ViewHolder holder;
        View view;
        switch (viewType) {
            case VIEW_TYPE_HEADER:
                view = LayoutInflater.from(mContext).inflate(R.layout.friend_item_header, parent, false);
                holder = new ViewHolder(view);
                break;
            case VIEW_TYPE_ITEM:
                view = LayoutInflater.from(mContext).inflate(R.layout.friend_item_main, parent, false);
                holder = new ViewHolder(view);
                holder.view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Friend friend = mFriendList.get(holder.getAdapterPosition() - mPendingFriendList.size() - (mPendingFriendList.size() == 0 ? 1 : 2));
                        if (!friend.isOnline()) return;
                        Chat chat = mApp.findChat(friend.getEmail());
                        if (chat == null) chat = new Chat(friend.getEmail());
                        else chat.unreadCount = 0;
                        Intent intent = new Intent(mContext, ChatDetailActivity.class);
                        intent.putExtra("chat_data", chat);
                        mContext.startActivity(intent);
                    }
                });
                break;
            default:
                view = LayoutInflater.from(mContext).inflate(R.layout.friend_item_pending, parent, false);
                holder = new ViewHolder(view);
                holder.accept.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final String pendingFriend = mPendingFriendList.get(holder.getAdapterPosition() - 1);
                        mApp.emitAcceptFriendRequest(pendingFriend, new Ack() {
                            @Override
                            public void call(Object... args) {
                                String publicKeyPEM = mApp.decryptForServerSession((String) args[0]);
                                String status = mApp.decryptForServerSession((String) args[1]);
                                Friend friend = new Friend(pendingFriend, publicKeyPEM, status.equals("online"));
                                mApp.pendingFriendList.remove(holder.getAdapterPosition() - 1);
                                mApp.friendList.add(friend);
                                mApp.mainActivity.notifyFriendsChanged();
                            }
                        });
                    }
                });
                holder.deny.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String pendingFriend = mPendingFriendList.get(holder.getAdapterPosition() - 1);
                        mApp.emitDenyFriendRequest(pendingFriend);
                        mApp.pendingFriendList.remove(holder.getAdapterPosition() - 1);
                        notifyDataSetChanged();
                    }
                });
                break;
        }
        return holder;
    }

    @Override
    public void onBindHeaderViewHolder(ViewHolder holder, int section, boolean expanded) {
        switch (section) {
            case 0:
                holder.title.setText(R.string.pending_friend_requests);
                break;
            case 1:
                holder.title.setText(R.string.friends);
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int section, int relativePosition, int absolutePosition) {
        switch (section) {
            case 0:
                holder.email.setText(mPendingFriendList.get(relativePosition));
                break;
            case 1:
                Friend friend = mFriendList.get(relativePosition);
                holder.email.setText(friend.getEmail());
                holder.email.setTextColor(friend.isOnline() ? Color.WHITE : Color.GRAY);
                break;
        }
    }

    @Override
    public int getItemViewType(int section, int relativePosition, int absolutePosition) {
        if (section == 0) return 0;
        return super.getItemViewType(section, relativePosition, absolutePosition);
    }

    @Override
    public int getSectionCount() {
        return 2;
    }

    @Override
    public int getItemCount(int section) {
        switch (section) {
            case 0: return mPendingFriendList.size();
            case 1: return mFriendList.size();
        }
        return 0;
    }

}
