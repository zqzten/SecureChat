package com.akikoz.securechat.util;

import android.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.Charset;
import java.security.SecureRandom;

public class AESUtil {

    private static final String AES_CBC_PKCS5 = "AES/CBC/PKCS5Padding";
    private static final SecureRandom secureRandom = new SecureRandom();

    public static byte[] encrypt(byte[] plain, String key, String iv) {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(Base64.decode(key, Base64.DEFAULT), AES_CBC_PKCS5);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(Base64.decode(iv, Base64.DEFAULT));
            Cipher cipher = Cipher.getInstance(AES_CBC_PKCS5);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
            return cipher.doFinal(plain);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] decrypt(byte[] encrypted, String key, String iv) {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(Base64.decode(key, Base64.DEFAULT), AES_CBC_PKCS5);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(Base64.decode(iv, Base64.DEFAULT));
            Cipher cipher = Cipher.getInstance(AES_CBC_PKCS5);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
            return cipher.doFinal(encrypted);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String encrypt(String plain, String key, String iv) {
        return Base64.encodeToString(encrypt(plain.getBytes(), key, iv), Base64.DEFAULT);
    }

    public static String decrypt(String encrypted, String key, String iv) {
        return new String(decrypt(Base64.decode(encrypted, Base64.DEFAULT), key, iv), Charset.defaultCharset());
    }

    public static String randomAESBytesEncoded() {
        byte [] randomBytes = new byte[16];
        secureRandom.nextBytes(randomBytes);
        return Base64.encodeToString(randomBytes, Base64.DEFAULT);
    }

}
