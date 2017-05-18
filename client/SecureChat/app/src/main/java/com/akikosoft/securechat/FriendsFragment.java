package com.akikosoft.securechat;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.*;
import android.widget.EditText;
import android.widget.Toast;
import com.akikosoft.securechat.adapter.FriendAdapter;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;
import io.socket.client.Ack;

public class FriendsFragment extends Fragment {

    private SecureChatApplication app;
    private FriendAdapter friendAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        app = (SecureChatApplication) getActivity().getApplication();
        friendAdapter = new FriendAdapter(app);
        View view = inflater.inflate(R.layout.recycler_view, container, false);
        RecyclerView recyclerView = (RecyclerView) view;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(friendAdapter);
        recyclerView.addItemDecoration(new HorizontalDividerItemDecoration.Builder(getContext()).build());
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        notifyChanged();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.friends_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add_friend:
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle(R.string.add_friend);
                final EditText emailEdit = new EditText(getContext());
                emailEdit.setHint(R.string.add_friend_input_hint);
                builder.setView(emailEdit);
                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String email = emailEdit.getText().toString();
                        if (TextUtils.isEmpty(email)) return;
                        app.emitFriendRequest(email, new Ack() {
                            @Override
                            public void call(Object... args) {
                                String status = app.decryptForServerSession((String) args[0]);
                                int infoText = 0;
                                switch (status) {
                                    case "success": infoText = R.string.friend_request_sent; break;
                                    case "no such user": infoText = R.string.no_such_user; break;
                                    case "friends already": infoText = R.string.friends_already; break;
                                }
                                final int finalInfoText = infoText;
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(getContext(), finalInfoText, Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                        });
                    }
                });
                builder.setNegativeButton(R.string.cancel, null);
                builder.show();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    void notifyChanged() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                friendAdapter.notifyDataSetChanged();
            }
        });
    }

}
