package com.akikoz.securechat.util;

import com.akikoz.securechat.model.Secret;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class RESTUtil {

    private static final String BASE_URL = "http://10.0.2.2:3000/api/";
    private static Secret secret;

    public static void setSecret(Secret secret) {
        RESTUtil.secret = secret;
    }

    public static void securePOST(String api, String body, okhttp3.Callback callback) {
        OkHttpClient client = new OkHttpClient();
        FormBody.Builder bodyBuilder = new FormBody.Builder();
        bodyBuilder.add("key", RSAUtil.encrypt(secret.getAesKey(), secret.getPublicKey()));
        bodyBuilder.add("iv", RSAUtil.encrypt(secret.getAesIV(), secret.getPublicKey()));
        bodyBuilder.add("body", AESUtil.encrypt(body, secret.getAesKey(), secret.getAesIV()));
        Request request = new Request.Builder().url(BASE_URL + api).post(bodyBuilder.build()).build();
        client.newCall(request).enqueue(callback);
    }

    public static String getPlainBody(String body) {
        return AESUtil.decrypt(body, secret.getAesKey(), secret.getAesIV());
    }

}
