package com.akikoz.securechat.util;

import android.util.Base64;

import javax.crypto.Cipher;
import java.nio.charset.Charset;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class RSAUtil {

    private static final String RSA = "RSA";
    private static final String RSA_PKCS1 = "RSA/NONE/PKCS1Padding";

    public static String encrypt(String plain, Key key) {
        try {
            Cipher cipher = Cipher.getInstance(RSA_PKCS1);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return Base64.encodeToString(cipher.doFinal(plain.getBytes()), Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String decrypt(String encrypted, Key key) {
        try {
            Cipher cipher = Cipher.getInstance(RSA_PKCS1);
            cipher.init(Cipher.DECRYPT_MODE, key);
            return new String(cipher.doFinal(Base64.decode(encrypted, Base64.DEFAULT)), Charset.defaultCharset());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static PublicKey getPublicFromPEM(String pemStr) {
        try {
            pemStr = pemStr.replace("-----BEGIN PUBLIC KEY-----\n", "");
            pemStr = pemStr.replace("-----END PUBLIC KEY-----", "");
            byte[] keyData = Base64.decode(pemStr, Base64.DEFAULT);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyData);
            return KeyFactory.getInstance(RSA).generatePublic(keySpec);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static PrivateKey getPrivateFromPEM(String pemStr) {
        try {
            pemStr = pemStr.replace("-----BEGIN RSA PRIVATE KEY-----\n", "");
            pemStr = pemStr.replace("-----END RSA PRIVATE KEY-----", "");
            byte[] keyData = Base64.decode(pemStr, Base64.DEFAULT);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyData);
            return KeyFactory.getInstance(RSA).generatePrivate(keySpec);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
