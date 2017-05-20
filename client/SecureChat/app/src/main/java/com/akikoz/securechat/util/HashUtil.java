package com.akikoz.securechat.util;

import android.util.Base64;

import java.security.MessageDigest;

public class HashUtil {

    private static final String SHA256 = "SHA-256";

    public static String calcHash(byte[] data) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(SHA256);
            messageDigest.update(data);
            return Base64.encodeToString(messageDigest.digest(), Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
