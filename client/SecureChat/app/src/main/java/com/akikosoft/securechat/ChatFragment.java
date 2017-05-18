package com.akikosoft.securechat;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.akikosoft.securechat.adapter.ChatAdapter;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;

public class ChatFragment extends Fragment {

    private SecureChatApplication app;
    private ChatAdapter chatAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        app = (SecureChatApplication) getActivity().getApplication();
        chatAdapter = new ChatAdapter(app.chatList);
        View view = inflater.inflate(R.layout.recycler_view, container, false);
        RecyclerView recyclerView = (RecyclerView) view;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(chatAdapter);
        recyclerView.addItemDecoration(new HorizontalDividerItemDecoration.Builder(getContext()).build());
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        chatAdapter.notifyDataSetChanged();
    }

    void notifyItemAppended() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                chatAdapter.notifyItemInserted(app.chatList.size() - 1);
            }
        });
    }

    void notifyItemChanged(final int position) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                chatAdapter.notifyItemChanged(position);
            }
        });
    }

    void notifyItemRemoved(final int position) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                chatAdapter.notifyItemRemoved(position);
            }
        });
    }

}
