package com.akikoz.securechat.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.akikoz.securechat.R;
import com.akikoz.securechat.model.Msg;
import com.akikoz.securechat.model.MsgType;

import java.util.List;

public class MsgAdapter extends RecyclerView.Adapter<MsgAdapter.ViewHolder> {

    private List<Msg> mMsgList;

    static class ViewHolder extends RecyclerView.ViewHolder {

        LinearLayout leftLayout;
        LinearLayout rightLayout;

        TextView leftMsg;
        TextView rightMsg;

        ImageView leftFileIndicator;
        ImageView rightFileIndicator;

        ViewHolder(View view) {
            super(view);
            leftLayout = (LinearLayout) view.findViewById(R.id.left_layout);
            rightLayout = (LinearLayout) view.findViewById(R.id.right_layout);
            leftMsg = (TextView) view.findViewById(R.id.left_msg);
            rightMsg = (TextView) view.findViewById(R.id.right_msg);
            leftFileIndicator = (ImageView) view.findViewById(R.id.left_file_indicator);
            rightFileIndicator = (ImageView) view.findViewById(R.id.right_file_indicator);
        }

    }

    public MsgAdapter(List<Msg> msgList) {
        mMsgList = msgList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.msg_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Msg msg = mMsgList.get(position);
        MsgType type = msg.getType();
        if (type == MsgType.TEXT_RECEIVED || type == MsgType.FILE_RECEIVED) {
            holder.leftLayout.setVisibility(View.VISIBLE);
            holder.rightLayout.setVisibility(View.GONE);
            holder.leftMsg.setText(msg.getContent());
            holder.leftFileIndicator.setVisibility(type == MsgType.TEXT_RECEIVED ? View.GONE : View.VISIBLE);
        } else if (type == MsgType.TEXT_SENT || type == MsgType.FILE_SENT) {
            holder.leftLayout.setVisibility(View.GONE);
            holder.rightLayout.setVisibility(View.VISIBLE);
            holder.rightMsg.setText(msg.getContent());
            holder.rightFileIndicator.setVisibility(type == MsgType.TEXT_SENT ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return mMsgList.size();
    }

}
