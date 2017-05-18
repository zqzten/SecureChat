package com.akikosoft.securechat.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Chat implements Serializable {

    private String person;
    public Secret secret;
    public List<Msg> msgList;
    public int unreadCount;

    public Chat(String person) {
        this.person = person;
        msgList = new ArrayList<>();
    }

    public String getPerson() {
        return person;
    }

}
