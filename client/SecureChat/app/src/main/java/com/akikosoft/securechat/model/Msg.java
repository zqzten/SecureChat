package com.akikosoft.securechat.model;

import java.io.Serializable;

public class Msg implements Serializable {

    private MsgType type;
    private String content; // raw text or file name

    public Msg(MsgType type, String content) {
        this.type = type;
        this.content = content;
    }

    public MsgType getType() {
        return type;
    }

    public String getContent() {
        return content;
    }

}
