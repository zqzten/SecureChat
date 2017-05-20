package com.akikoz.securechat;

import android.app.Application;
import android.os.Environment;
import com.akikoz.securechat.model.*;
import com.akikoz.securechat.util.*;
import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

public class SecureChatApplication extends Application {

    private Socket socket;
    {
        try {
            socket = IO.socket("http://10.0.2.2:3000/");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private Secret serverSecret;

    public String userEmail;
    public String userPassword;
    public PrivateKey userPrivateKey;
    public List<Chat> chatList;
    public List<Friend> friendList;
    public List<String> pendingFriendList;

    public MainActivity mainActivity;
    public ChatDetailActivity chatDetailActivity;

    @Override
    public void onCreate() {
        super.onCreate();

        // load server public key
        PublicKey serverPublicKey = null;
        try {
            InputStream is = getAssets().open("server_public.pem");
            byte[] data = new byte[is.available()];
            is.read(data);
            is.close();
            serverPublicKey = RSAUtil.getPublicFromPEM(new String(data, Charset.defaultCharset()));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // make secret for communication with server
        serverSecret = new Secret(serverPublicKey, AESUtil.randomAESBytesEncoded(), AESUtil.randomAESBytesEncoded());

        // pass server secret to RESTUtil
        RESTUtil.setSecret(serverSecret);

        // init user info
        initUserInfo();
    }

    void initUserInfo() {
        userEmail = null;
        userPassword = null;
        userPrivateKey = null;
        chatList = new ArrayList<>();
        friendList = new ArrayList<>();
        pendingFriendList = new ArrayList<>();
    }

    public Chat findChat(String email) {
        for (Chat chat : chatList)
            if (chat.getPerson().equals(email))
                return chat;
        return null;
    }

    public Friend findFriend(String email) {
        for (Friend friend : friendList)
            if (friend.getEmail().equals(email))
                return friend;
        return null;
    }

    public String encryptForServerSession(String plain) {
        return AESUtil.encrypt(plain, serverSecret.getAesKey(), serverSecret.getAesIV());
    }

    public String decryptForServerSession(String encrypted) {
        return AESUtil.decrypt(encrypted, serverSecret.getAesKey(), serverSecret.getAesIV());
    }

    public String signHash(byte[] data) {
        return RSAUtil.encrypt(HashUtil.calcHash(data), userPrivateKey);
    }

    public boolean validateSignature(byte[] data, String signature, PublicKey publicKey) {
        return RSAUtil.decrypt(signature, publicKey).equals(HashUtil.calcHash(data));
    }

    public void connectSocket() {
        socket.connect();
    }

    public void emitOnline() {
        socket.emit("online", encryptForServerSession(userEmail), encryptForServerSession(userPassword), RSAUtil.encrypt(serverSecret.getAesKey(), serverSecret.getPublicKey()), RSAUtil.encrypt(serverSecret.getAesIV(), serverSecret.getPublicKey()));
    }

    public void emitOffline() {
        socket.emit("offline");
    }

    public void emitFriendRequest(String email, Ack ack) {
        socket.emit("friend request", encryptForServerSession(email), ack);
    }

    public void emitAcceptFriendRequest(String email, Ack ack) {
        socket.emit("accept friend request", encryptForServerSession(email), ack);
    }

    public void emitDenyFriendRequest(String email) {
        socket.emit("deny friend request", encryptForServerSession(email));
    }

    public void emitInitialTextMsg(String email, String encryptedKey, String encryptedIV, String encryptedContent, String encryptedSignature) {
        socket.emit("initial text msg", encryptForServerSession(email), encryptedKey, encryptedIV, encryptedContent, encryptedSignature);
    }

    public void emitTextMsg(String email, String encryptedContent, String encryptedSignature) {
        socket.emit("text msg", encryptForServerSession(email), encryptedContent, encryptedSignature);
    }

    public void emitInitialFileMsg(String email, String encryptedKey, String encryptedIV, String encryptedFileName, byte[] encryptedContent, String encryptedSignature) {
        socket.emit("initial file msg", encryptForServerSession(email), encryptedKey, encryptedIV, encryptedFileName, encryptedContent, encryptedSignature);
    }

    public void emitFileMsg(String email, String encryptedFileName, byte[] encryptedContent, String encryptedSignature) {
        socket.emit("file msg", encryptForServerSession(email), encryptedFileName, encryptedContent, encryptedSignature);
    }

    public void bindSocketEvents() {
        socket.on("test", onTest);
        socket.on("friends", onFriends);
        socket.on("friend online", onFriendOnline);
        socket.on("friend offline", onFriendOffline);
        socket.on("new friend request", onNewFriendRequest);
        socket.on("friend request accepted", onFriendRequestAccepted);
        socket.on("initial text msg received", onInitialTextMsgReceived);
        socket.on("text msg received", onTextMsgReceived);
        socket.on("initial file msg received", onInitialFileMsgReceived);
        socket.on("file msg received", onFileMsgReceived);
    }

    public void unbindSocketEvents() {
        socket.off("test", onTest);
        socket.off("friends", onFriends);
        socket.off("friend online", onFriendOnline);
        socket.off("friend offline", onFriendOffline);
        socket.off("new friend request", onNewFriendRequest);
        socket.off("friend request accepted", onFriendRequestAccepted);
        socket.off("initial text msg received", onInitialTextMsgReceived);
        socket.off("text msg received", onTextMsgReceived);
        socket.off("initial file msg received", onInitialFileMsgReceived);
        socket.off("file msg received", onFileMsgReceived);
    }

    private Emitter.Listener onTest = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            System.out.println(AESUtil.decrypt((String) args[0], serverSecret.getAesKey(), serverSecret.getAesIV()));
        }
    };

    private Emitter.Listener onFriends = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            try {
                JSONObject friends = new JSONObject(decryptForServerSession((String) args[0]));
                JSONArray friendListJSON = friends.getJSONArray("friendList");
                JSONArray pendingFriendListJSON = friends.getJSONArray("pendingFriendList");
                for (int i = 0; i < friendListJSON.length(); i++) {
                    JSONObject friendJSON = friendListJSON.getJSONObject(i);
                    Friend friend = new Friend(friendJSON.getString("email"), friendJSON.getString("publicKeyPEM"), friendJSON.getBoolean("online"));
                    friendList.add(friend);
                }
                for (int i = 0; i < pendingFriendListJSON.length(); i++) {
                    pendingFriendList.add(pendingFriendListJSON.getString(i));
                }
                if (mainActivity != null) mainActivity.notifyFriendsChanged();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    private Emitter.Listener onFriendOnline = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            String email = decryptForServerSession((String) args[0]);
            findFriend(email).setOnline(true);
            if (mainActivity != null) mainActivity.notifyFriendsChanged();
        }
    };

    private Emitter.Listener onFriendOffline = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            String email = decryptForServerSession((String) args[0]);
            findFriend(email).setOnline(false);
            int chatIndex = chatList.indexOf(findChat(email));
            if (chatIndex != -1) chatList.remove(chatIndex);
            if (mainActivity != null) {
                if (chatIndex != -1) mainActivity.notifyChatItemRemoved(chatIndex);
                mainActivity.notifyFriendsChanged();
            }
            if (chatDetailActivity != null && chatDetailActivity.chat.getPerson().equals(email)) chatDetailActivity.finish();
        }
    };

    private Emitter.Listener onNewFriendRequest = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            String email = decryptForServerSession((String) args[0]);
            pendingFriendList.add(email);
            if (mainActivity != null) mainActivity.notifyFriendsChanged();
        }
    };

    private Emitter.Listener onFriendRequestAccepted = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            String email = decryptForServerSession((String) args[0]);
            String publicKeyPEM = decryptForServerSession((String) args[1]);
            Friend friend = new Friend(email, publicKeyPEM, true);
            friendList.add(friend);
            if (mainActivity != null) mainActivity.notifyFriendsChanged();
        }
    };

    private Emitter.Listener onInitialTextMsgReceived = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            // decrypt
            String email = decryptForServerSession((String) args[0]);
            String aesKey = RSAUtil.decrypt((String) args[1], userPrivateKey);
            String aesIV = RSAUtil.decrypt((String) args[2], userPrivateKey);
            String content = AESUtil.decrypt((String) args[3], aesKey, aesIV);
            String signature = AESUtil.decrypt((String) args[4], aesKey, aesIV);
            // get public key
            PublicKey publicKey = RSAUtil.getPublicFromPEM(findFriend(email).getPublicKeyPEM());
            // validate signature
            if (!validateSignature(content.getBytes(), signature, publicKey)) return;
            // init chat
            Chat chat = new Chat(email);
            chat.secret = new Secret(publicKey, aesKey, aesIV);
            Msg msg = new Msg(MsgType.TEXT_RECEIVED, content);
            chat.msgList.add(msg);
            chat.unreadCount++;
            chatList.add(chat);
            if (mainActivity != null) mainActivity.notifyChatItemAppended();
        }
    };

    private Emitter.Listener onTextMsgReceived = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            // decrypt
            String email = decryptForServerSession((String) args[0]);
            Chat chat = findChat(email);
            String content = AESUtil.decrypt((String) args[1], chat.secret.getAesKey(), chat.secret.getAesIV());
            String signature = AESUtil.decrypt((String) args[2], chat.secret.getAesKey(), chat.secret.getAesIV());
            // validate signature
            if (!validateSignature(content.getBytes(), signature, chat.secret.getPublicKey())) return;
            // append msg
            Msg msg = new Msg(MsgType.TEXT_RECEIVED, content);
            appendMsg(chat, msg);
        }
    };

    private Emitter.Listener onInitialFileMsgReceived = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            // decrypt
            String email = decryptForServerSession((String) args[0]);
            String aesKey = RSAUtil.decrypt((String) args[1], userPrivateKey);
            String aesIV = RSAUtil.decrypt((String) args[2], userPrivateKey);
            String fileName = AESUtil.decrypt((String) args[3], aesKey, aesIV);
            byte[] content = AESUtil.decrypt((byte[]) args[4], aesKey, aesIV);
            String signature = AESUtil.decrypt((String) args[5], aesKey, aesIV);
            // get public key
            PublicKey publicKey = RSAUtil.getPublicFromPEM(findFriend(email).getPublicKeyPEM());
            // validate signature
            if (!validateSignature(FileUtil.combineBytes(fileName.getBytes(), content), signature, publicKey)) return;
            try {
                // save to file
                saveToFile(fileName, content);
                // init chat
                Chat chat = new Chat(email);
                chat.secret = new Secret(publicKey, aesKey, aesIV);
                Msg msg = new Msg(MsgType.FILE_RECEIVED, fileName);
                chat.msgList.add(msg);
                chat.unreadCount++;
                chatList.add(chat);
                if (mainActivity != null) mainActivity.notifyChatItemAppended();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    private Emitter.Listener onFileMsgReceived = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            // decrypt
            String email = decryptForServerSession((String) args[0]);
            Chat chat = findChat(email);
            String fileName = AESUtil.decrypt((String) args[1], chat.secret.getAesKey(), chat.secret.getAesIV());
            byte[] content = AESUtil.decrypt((byte[]) args[2], chat.secret.getAesKey(), chat.secret.getAesIV());
            String signature = AESUtil.decrypt((String) args[3], chat.secret.getAesKey(), chat.secret.getAesIV());
            // validate signature
            if (!validateSignature(FileUtil.combineBytes(fileName.getBytes(), content), signature, chat.secret.getPublicKey())) return;
            try {
                // save to file
                saveToFile(fileName, content);
                // append msg
                Msg msg = new Msg(MsgType.FILE_RECEIVED, fileName);
                appendMsg(chat, msg);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    private void appendMsg(Chat chat, Msg msg) {
        chat.msgList.add(msg);
        if (chatDetailActivity != null && chatDetailActivity.chat.getPerson().equals(chat.getPerson())) {
            if (chat != chatDetailActivity.chat) chatDetailActivity.chat.msgList.add(msg);
            chatDetailActivity.notifyItemAppended();
        } else if (mainActivity != null) {
            chat.unreadCount++;
            mainActivity.notifyChatItemChanged(chatList.indexOf(chat));
        }
    }

    private void saveToFile(String fileName, byte[] content) throws IOException {
        File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
        FileOutputStream fos = new FileOutputStream(f);
        fos.write(content);
        fos.close();
    }

}
