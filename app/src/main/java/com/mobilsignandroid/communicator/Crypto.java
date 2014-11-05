package com.mobilsignandroid.communicator;

import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;

public class Crypto {

    public static final String ALGORITHM = "RSA/ECB/NOPADDING";
    private Cipher mCipher;
    private Key mKey;

    // Parameter key je bud PublicKey alebo PrivateKey.
    public Crypto(Key key) {
        mKey = key;
        try {
            mCipher = Cipher.getInstance(ALGORITHM);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException ex) {
            Logger.getLogger(Crypto.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void setKey(Key key) {
        mKey = key;
    }
    
    public static byte[] encrypt(byte[] data, Key key) {
        byte[] encryptedData = null;
        try {
            final Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            encryptedData = cipher.doFinal(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return encryptedData;
    }
    
    public static byte[] decrypt(byte[] data, Key key) {
        byte[] dectyptedData = null;
        try {
            final Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key);
            dectyptedData = cipher.doFinal(data);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return dectyptedData;
    }

    public String encrypt(String text) {
        //System.out.println("Encryptujem klucom: " + mKey);
        List<byte[]> chunks = Util.divideByteArray(text.getBytes(), 245);
        List<String> zasifrovaneChunks = new ArrayList<>();
        for (byte[] iter : chunks) {
            zasifrovaneChunks.add(Util.encodeByteToBase64String(this.encryptChunk(iter)));
        }
        StringBuilder builder = new StringBuilder();
        for (String iter : zasifrovaneChunks) {
            builder.append(iter.trim());
    }
        return builder.toString();
    }

    public String decrypt(String text) {
        //System.out.println("Decryptujem klucom: " + mKey);
        List<byte[]> chunks = Util.divideByteArray(text.getBytes(), 344);
        List<byte[]> rozsifrovaneChunks = new ArrayList<>();
        for (byte[] iter : chunks) {
            rozsifrovaneChunks.add(this.decryptChunk(Util.decodeBase64StringToByte(new String(iter))));
        }
        StringBuilder builder = new StringBuilder();
        for (byte[] iter : rozsifrovaneChunks) {
            builder.append(new String(iter).trim());
    }
        return builder.toString();
    }

    public String encryptFile(byte[] subor) {
        List<byte[]> chunks = Util.divideByteArray(subor, 245);
        List<String> zasifrovaneChunks = new ArrayList<>();
        for (byte[] iter : chunks) {
            zasifrovaneChunks.add(Util.encodeByteToBase64String(this.encryptChunk(iter)));
        }
        StringBuilder builder = new StringBuilder();
        for (String iter : zasifrovaneChunks) {
            builder.append(iter);
        }
        return builder.toString();
    }

    public byte[] decryptFile(String subor) {
        List<byte[]> chunks = Util.divideByteArray(subor.getBytes(), 344);
        List<byte[]> rozsifrovaneChunks = new ArrayList<>();
        for (byte[] iter : chunks) {
            rozsifrovaneChunks.add(this.decryptChunk(Util.decodeBase64StringToByte(new String(iter))));
        }
        return Util.splitByteArray(rozsifrovaneChunks, 245);
    }

    /**
     * Encrypt the plain text using public key.
     * 
     * MALO BY TO BYT TO ISTE CO ENCRYPT, treba otestovat 
     *
     * @param text : original plain text
     * @param key :The public key
     * @return Encrypted text
     * @throws java.lang.Exception
     */
    private byte[] encryptChunk(byte[] text) {
        byte[] cipherText = null;
        try {
            // get an RSA cipher object and print the provider
            final Cipher cipher = Cipher.getInstance(ALGORITHM);
            // encrypt the plain text using the public key
            cipher.init(Cipher.ENCRYPT_MODE, mKey);
            cipherText = cipher.doFinal(text);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cipherText;
    }

    /**
     * Decrypt text using private key.
     *
     * MALO BY TO BYT TO ISTE CO DECRYPT, treba otestovat
     *
     * @param text :encrypted text
     * @param key :The private key
     * @return plain text
     * @throws java.lang.Exception
     */
    private byte[] decryptChunk(byte[] text) {
        byte[] dectyptedText = null;
        try {
            // get an RSA cipher object and print the provider
            final Cipher cipher = Cipher.getInstance(ALGORITHM);

            // decrypt the text using the private key
            cipher.init(Cipher.DECRYPT_MODE, mKey);
            dectyptedText = cipher.doFinal(text);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return dectyptedText;
    }

}
