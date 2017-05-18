package com.akikosoft.securechat.model;

public class Friend {

    private String email;
    private boolean online;
    private String publicKeyPEM;

    public Friend(String email, String publicKeyPEM, boolean online) {
        this.email = email;
        this.publicKeyPEM = publicKeyPEM;
        this.online = online;
    }

    public String getEmail() {
        return email;
    }

    public String getPublicKeyPEM() {
        return publicKeyPEM;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

}
