package com.akikosoft.securechat;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import com.akikosoft.securechat.adapter.MsgAdapter;
import com.akikosoft.securechat.model.*;
import com.akikosoft.securechat.util.AESUtil;
import com.akikosoft.securechat.util.FileUtil;
import com.akikosoft.securechat.util.RSAUtil;

import java.io.File;
import java.security.PublicKey;

public class ChatDetailActivity extends AppCompatActivity {

    private static final int FILE_SELECTION_CODE = 0;

    private SecureChatApplication app;
    private RecyclerView recyclerView;
    private MsgAdapter msgAdapter;
    private EditText inputText;

    Chat chat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_detail);

        app = (SecureChatApplication) getApplication();
        chat = (Chat) getIntent().getSerializableExtra("chat_data");

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(chat.getPerson());
        }

        recyclerView = (RecyclerView) findViewById(R.id.msg_recycler_view);
        msgAdapter = new MsgAdapter(chat.msgList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(msgAdapter);

        inputText = (EditText) findViewById(R.id.input_text);
        Button sendText = (Button) findViewById(R.id.send_text);
        sendText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String content = inputText.getText().toString();
                if (TextUtils.isEmpty(content)) return;
                if (chat.secret == null) {
                    // first msg
                    initChatSecret();
                    secureEmitTextMsg(content, true);
                    Msg msg = new Msg(MsgType.TEXT_SENT, content);
                    chat.msgList.add(msg);
                    app.chatList.add(chat);
                } else {
                    // not first msg
                    secureEmitTextMsg(content, false);
                    Msg msg = new Msg(MsgType.TEXT_SENT, content);
                    appendMsg(msg);
                }
                notifyItemAppended();
                inputText.setText("");
            }
        });
        ImageButton sendFile = (ImageButton) findViewById(R.id.send_file);
        sendFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                try {
                    startActivityForResult(Intent.createChooser(intent, "Select file"), FILE_SELECTION_CODE);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(ChatDetailActivity.this, R.string.external_file_chooser_needed, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        app.chatDetailActivity = this;
    }

    @Override
    protected void onResume() {
        super.onResume();
        msgAdapter.notifyDataSetChanged();
        if (chat.msgList.size() > 0) recyclerView.scrollToPosition(chat.msgList.size() - 1);
    }

    @Override
    protected void onStop() {
        super.onStop();
        app.chatDetailActivity = null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == FILE_SELECTION_CODE) {
            Uri uri = data.getData();
            File file = new File(FileUtil.getPath(this, uri));
            try {
                String fileName = file.getName();
                byte[] content = FileUtil.fullyReadFileToBytes(file);
                if (chat.secret == null) {
                    // first msg
                    initChatSecret();
                    secureEmitFileMsg(fileName, content, true);
                    Msg msg = new Msg(MsgType.FILE_SENT, fileName);
                    chat.msgList.add(msg);
                    app.chatList.add(chat);
                } else {
                    // not first msg
                    secureEmitFileMsg(fileName, content, false);
                    Msg msg = new Msg(MsgType.FILE_SENT, fileName);
                    appendMsg(msg);
                }
                notifyItemAppended();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, R.string.failed_to_read_the_file, Toast.LENGTH_LONG).show();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    void notifyItemAppended() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                msgAdapter.notifyItemInserted(chat.msgList.size() - 1);
                if (chat.msgList.size() > 0) recyclerView.scrollToPosition(chat.msgList.size() - 1);
            }
        });
    }

    private void initChatSecret() {
        PublicKey publicKey = RSAUtil.getPublicFromPEM(app.findFriend(chat.getPerson()).getPublicKeyPEM());
        String aesKey = AESUtil.randomAESBytesEncoded();
        String aesIV = AESUtil.randomAESBytesEncoded();
        chat.secret = new Secret(publicKey, aesKey, aesIV);
    }

    private void appendMsg(Msg msg) {
        chat.msgList.add(msg);
        Chat savedChat = app.findChat(chat.getPerson());
        if (savedChat != chat) savedChat.msgList.add(msg);
    }

    private void secureEmitTextMsg(String content, boolean initial) {
        // sign
        String signature = app.signHash(content.getBytes());
        // encrypt then emit
        String encryptedContent = AESUtil.encrypt(content, chat.secret.getAesKey(), chat.secret.getAesIV());
        String encryptedSignature = AESUtil.encrypt(signature, chat.secret.getAesKey(), chat.secret.getAesIV());
        if (initial) {
            String encryptedKey = RSAUtil.encrypt(chat.secret.getAesKey(), chat.secret.getPublicKey());
            String encryptedIV = RSAUtil.encrypt(chat.secret.getAesIV(), chat.secret.getPublicKey());
            app.emitInitialTextMsg(chat.getPerson(), encryptedKey, encryptedIV, encryptedContent, encryptedSignature);
        } else {
            app.emitTextMsg(chat.getPerson(), encryptedContent, encryptedSignature);
        }
    }

    private void secureEmitFileMsg(String fileName, byte[] content, boolean initial) {
        // sign
        String signature = app.signHash(FileUtil.combineBytes(fileName.getBytes(), content));
        // encrypt then emit
        String encryptedFileName = AESUtil.encrypt(fileName, chat.secret.getAesKey(), chat.secret.getAesIV());
        byte[] encryptedContent = AESUtil.encrypt(content, chat.secret.getAesKey(), chat.secret.getAesIV());
        String encryptedSignature = AESUtil.encrypt(signature, chat.secret.getAesKey(), chat.secret.getAesIV());
        if (initial) {
            String encryptedKey = RSAUtil.encrypt(chat.secret.getAesKey(), chat.secret.getPublicKey());
            String encryptedIV = RSAUtil.encrypt(chat.secret.getAesIV(), chat.secret.getPublicKey());
            app.emitInitialFileMsg(chat.getPerson(), encryptedKey, encryptedIV, encryptedFileName, encryptedContent, encryptedSignature);
        } else {
            app.emitFileMsg(chat.getPerson(), encryptedFileName, encryptedContent, encryptedSignature);
        }
    }

}
