package com.example.insecurecommunicationserver;

import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptData {
    SecureRandom random;
    SecretKey secretKey;


    public EncryptData() {
        random = new SecureRandom();
        secretKey = null;
    }


    private String getSha256Hash(String password) {
        return bin2hex(getSha256HashInBytes(password));
    }

    private byte[] getSha256HashInBytes(String password) {
        try {
            MessageDigest digest = null;
            try {
                digest = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException e1) {
                e1.printStackTrace();
            }
            digest.reset();
            return digest.digest(password.getBytes());
        } catch (Exception ignored) {
            return null;
        }
    }

    private String bin2hex(byte[] data) {
        StringBuilder hex = new StringBuilder(data.length * 2);
        for (byte b : data)
            hex.append(String.format("%02x", b & 0xFF));
        return hex.toString();
    }

    public void setSecretKeyFromString(String inputPass) {
        byte[] passHash = this.getSha256HashInBytes(inputPass);
        secretKey = new SecretKeySpec(passHash, 0, passHash.length, "AES");
    }

    private byte[] generateIV() {
        byte[] IV = new byte[16];
        random.nextBytes(IV);
        return IV;
    }

    private byte[] encrypt(byte[] plaintext, byte[] IV) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec keySpec = new SecretKeySpec(secretKey.getEncoded(), "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(IV);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
        byte[] cipherText = cipher.doFinal(plaintext);
        return cipherText;
    }

    private String decrypt(byte[] cipherText, byte[] IV) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getEncoded(), "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(IV);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            byte[] decryptedText = cipher.doFinal(cipherText);
            return new String(decryptedText);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String encryptToSend(String plainText) {
        byte[] IV = generateIV();
        byte[] cipherText = null;
        try {
            cipherText = this.encrypt(plainText.getBytes(StandardCharsets.UTF_8), IV);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Base64.encodeToString(IV, Base64.NO_WRAP)+'$'+Base64.encodeToString(cipherText, Base64.NO_WRAP);
    }

    public String decryptFromRec(String recMsg) {
        if(recMsg==null)
            return "";
        String[] recMsgSplit = recMsg.split("\\$");
        if(recMsgSplit.length!=2)
            return "WRONG DATA RECV!";

        byte[] IV = Base64.decode(recMsgSplit[0], Base64.DEFAULT);
        byte[] cipherText = Base64.decode(recMsgSplit[1], Base64.DEFAULT);
        String plainText = null;
        try {
            plainText = this.decrypt(cipherText, IV);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return plainText;
    }
}
