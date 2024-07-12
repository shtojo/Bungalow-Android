package com.smj.bungalow;

import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import android.util.Base64;

public class Crypto {

    // Note: hash iterations: larger = more secure but slower (use >= 256)
    private static SecretKey secretKey = null;
    private static final int HASH_ITERATIONS = 256;
    @SuppressWarnings("unused")  // disable unused message for the AES mode not used below
    public enum Mode { AES128, AES256 }  // Encryption mode

    private static final String
            BLOCK_MODE = "AES/CBC/PKCS7Padding",
            PBE_ALGORITHM = "PBKDF2WithHmacSHA1",
            HASH_ALGORITHM = "SHA-256";

    /**
     * Generates a secure encryption key from a password and salt.
     * @param  password The password used to generate the secrete key.
     * @param  salt An 8 byte salt value (pass null if no salt)
     * @param  mode Security level (Mode.AES128 or Mode.AES256)
     */
    @SuppressWarnings("SameParameterValue")
    static void generateSecureKey(char[] password, byte[] salt, Mode mode)
            throws NoSuchAlgorithmException,
            InvalidKeySpecException, IllegalArgumentException {

        // Check for null or empty password
        if (password == null || password.length == 0) {
            throw new IllegalArgumentException("Crypto error, password is null!");
        }

        // Check salt (required with length 8)
        if ((salt == null) || (salt.length != 8)) {
            throw new IllegalArgumentException("Crypto error, bad salt length!");
        }

        // Add the salt and perform the hash using PBEKeySpec
        PBEKeySpec pbeKeySpec = new PBEKeySpec(password, salt,
                HASH_ITERATIONS, (mode == Mode.AES128) ? 128 : 256);

        // Done with password and salt so clear them from memory
        Arrays.fill(password, (char) 0);
        Arrays.fill(salt, (byte) 0);

        // Generate encryption key
        secretKey = new SecretKeySpec(SecretKeyFactory.getInstance(PBE_ALGORITHM)
                .generateSecret(pbeKeySpec).getEncoded(), "AES");
    }

    /**
     * Generates the encryption key from the password using a hash and AES
     * encryption. Should favor generateSecureKey method instead since that
     * method uses PBKDF2WithHmacSHA1 and is more secure.
     * @param  password The password used to generate the secrete key.
     * @param  salt An 8 byte salt value (pass null if no salt)
     * @param  mode Security level (Mode.AES128 or Mode.AES256)
     */
    @SuppressWarnings("unused")
    public static void generateHashKey(char[] password, byte[] salt, Mode mode)
            throws NoSuchAlgorithmException,
            IllegalArgumentException {

        // Check for null or empty password
        if (password == null || password.length == 0) {
            throw new IllegalArgumentException("Crypto error, password is null!");
        }

        // Check salt (allow null or length 8)
        if ((salt != null) && (salt.length != 8)) {
            throw new IllegalArgumentException("Crypto error, bad salt length!");
        }

        // Get a message digest that implements the specified algorithm and update digest
        byte[] mdkey;
        MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
        if (salt != null) {
            md.update(salt);
        }
        md.update(charsToBytes(password));
        mdkey = md.digest();

        // Done with password and salt so clear them from memory
        Arrays.fill(password, (char) 0);
        if (salt != null) {
            Arrays.fill(salt, (byte) 0);
        }

        // Shorten the key if using AES128 (instead of AES256)
        if (mode == Mode.AES128) {
            mdkey = Arrays.copyOf(mdkey, 16); // if aes128 then just use the first 128 bits
        }

        // Construct the secret key using the specified algorithm
        secretKey = new SecretKeySpec(mdkey, "AES");
    }

    /**
     * Encrypts a string with AES encryption and converts to base-64 for easy
     * transport over TCP.
     * @param  clearText The string to encrypt.
     * @return the encrypted string
     */
    static String encrypt(String clearText) throws NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException,
            BadPaddingException {
        if (clearText == null) return null;
        return encrypt(clearText.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Encrypts a byte array.
     * @param  clearText The byte array to encrypt.
     * @return the encrypted string or null if byte array is null or empty
     */
    @SuppressWarnings("WeakerAccess")
    static String encrypt(byte[] clearText)
            throws
            NoSuchAlgorithmException,
            NoSuchPaddingException,
            InvalidKeyException,
            IllegalBlockSizeException,
            BadPaddingException {

        if (clearText == null || clearText.length == 0) {
            return null;
        }

        // Get a cipher that implements the specified BLOCK_MODE ("AES/CBC/PKCS5Padding");
        Cipher cipher = Cipher.getInstance(BLOCK_MODE);

        // Initialize the cipher with the secret key, let it generate a random iv
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        // Encrypt the message
        byte[] cipherData = cipher.doFinal(clearText);

        // Get the new random iv to send to the receiver
        byte[] initVector = cipher.getIV();

        // Prepend the iv to the encrypted data
        byte[] fullPacket = new byte[initVector.length + cipherData.length];
        System.arraycopy(initVector, 0, fullPacket, 0, initVector.length);
        System.arraycopy(cipherData, 0, fullPacket, initVector.length, cipherData.length);

        // Encode base64
        return Base64.encodeToString(fullPacket, Base64.NO_WRAP);
    }

    // returns decrypted message, or null on failure (can call getLastErrorMessage)
    static String decrypt(String encryptedText)
        throws
        NoSuchPaddingException,             // by Cipher.getInstance
        NoSuchAlgorithmException,           // by Cipher.getInstance
        InvalidAlgorithmParameterException, // by cipher.init
        InvalidKeyException,                // by cipher.init
        BadPaddingException,                // by cipher.doFinal
        IllegalBlockSizeException {         // by cipher.doFinal

        if (encryptedText == null) return null;

        // Decode base64
        byte[] cipherText = Base64.decode(encryptedText, Base64.NO_WRAP);

        // Split out the iv (without encrypted data)
        byte[] initVector = new byte[16];
        System.arraycopy(cipherText, 0, initVector, 0, 16);

        // Split out the encrypted data (without iv)
        byte[] cipherData = new byte[cipherText.length - 16];
        System.arraycopy(cipherText, 16, cipherData, 0, cipherText.length - 16);

        // Get a cipher that implements the specified BLOCK_MODE ("AES/CBC/PKCS5Padding");
        Cipher cipher = Cipher.getInstance(BLOCK_MODE);
        // Initialize the cipher with the secret key and a algorithm
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(initVector));
        // Decrypt the message
        return new String(cipher.doFinal(cipherData), StandardCharsets.UTF_8);
    }

    /**
     * Converts a char array to a byte array
     * @param chars char array
     * @return byte array
     */
    private static byte[] charsToBytes(char[] chars) {
        byte[] bytes = new byte[chars.length];
        for (int i = 0; i < chars.length; i++) {
            bytes[i] = (byte)(chars[i]);
            chars[i] = (char)0;  // clear sensitive data
        }
        return bytes;
    }

    /*public static byte[] decrypt(String encryptedText) throws NoSuchAlgorithmException,
    NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException,
    UnsupportedEncodingException, IllegalBlockSizeException, BadPaddingException {
        if (encryptedText == null) return null;
        byte[] cipherText = Base64.decode(encryptedText, Base64.NO_WRAP);
        // split out the iv (without encrypted data)
        byte[] initVector = new byte[16];
        System.arraycopy(cipherText, 0, initVector, 0, 16);
        // split out the encrypted data (without iv)
        byte[] cipherData = new byte[cipherText.length - 16];
        System.arraycopy(cipherText, 16, cipherData, 0, cipherText.length - 16);
        Cipher cipher = Cipher.getInstance(blockMode);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(initVector));
        return cipher.doFinal(cipherData);
    }*/

    /*public static String byteArrayToHexString(byte[] raw) {
        if (raw == null) return null;
        final StringBuilder hex = new StringBuilder(2 * raw.length);
        for (final byte b : raw) {
            hex.append("0123456789ABCDEF".charAt((b & 0xF0) >> 4)).append(
                "0123456789ABCDEF".charAt((b & 0x0F)));
        }
        return hex.toString();
    }*/

    /*private static byte[] hexStringToByteArray(String hexString) {
        int len = hexString.length();
        byte[] ba = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            ba[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4) + Character.digit(
                hexString.charAt(i + 1), 16));
        }
        return ba;
    }*/

}

// encrypt():
// Encrypts a string with AES encryption then converts to Base64. Base64
// converts binary data
// to printable ascii characters and makes for easy transport via tcp. Note
// that some Base64
// encoders add newlines every 60 chars or so and the decoder ignores them,
// so I would need to
// use something else (like tab) for end marker or use the NOWRAP mode in
// the Base64 encoding
// instead of DEFAULT. Note that email uses Base64.
//
// A new initialization vector is required for each new encrypted packet and
// this iv must be
// sent along (unencrypted) with the encrypted data for every packet by
// prepending it to the
// encrypted data (prior to encoding to Base64). The receiver then uses this
// random data,
// along with the private password/key to decrypt the data. The iv is random
// data that must
// be known by both sides and must not be encrypted. It poses no security
// threat.
// The IV is always 16 bytes (128 bits) for all AES modes regardless if
// using AES128/192/256.
//
// note, using a string for salt reduces the number of possibilities for
// bytes since it is only printable chars. Should use base64 or hex string then convert to
// byte array so I can represent all values.
// byte[] hardSalt =
// {(byte)0x47,(byte)0x76,(byte)0x12,(byte)0xcf,(byte)0x56,(byte)0x87,(byte)0xfe,(byte)0x09};
//
// Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
// Security.addProvider(new com.sun.crypto.provider.SunJCE());
//
// Salt notes:
// Salt is a random 8 bytes that must be known to the server and the clients. It is used along
// with the password to ensure the generated private key is secure and not susceptible to
// dictionary attacks. The private key is generated from the combination of the password and the
// salt. The salt can be hard-coded and the same salt can be hard-coded in the server code.
// private static byte[] salt =
// {(byte)0x47,(byte)0x76,(byte)0x12,(byte)0xcf,(byte)0x56,(byte)0x87,(byte)0xfe,(byte)0x09};
//
// Generate a key from a password and optional salt. If salt is not provided, a key will be
// generated without salt both server and client must use the same parms (password and salt) to
// end up with the same key This class stores the key as global SecretKey and returns the key as
// a byte array. Password and salt should be generated and then given to both the server and
// clients. Salt should be 8 bytes in length. Generated key length the same length as aesBits
// (128 or 256) since key length determines the aes mode (128 or 256)

// Note:
// If not using encryption then no need for this class, just call this to convert to base64
// import android.util.Base64;
// Encrypt:
// Base64.encodeToString(my_string.getBytes("UTF-8"), Base64.NO_WRAP);  // if string or can send without base64
// Base64.encodeToString(my_byte_array, Base64.NO_WRAP);  // if byte array (must use base64 to convert binary data to a string)
// Decrypt:
// byte[] cipherText = Base64.decode(encryptedText, Base64.NO_WRAP);