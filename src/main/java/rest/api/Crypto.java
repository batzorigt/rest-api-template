package rest.api;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang3.StringUtils;

public interface Crypto {

    public static final String signAlgorithm = "HmacSHA256";
    public static final String transformation = "AES/CBC/PKCS5Padding";
    public static final String encryptionAlgorithm = "AES";

    private static Key generateKey(String secret, String algorithm) {
        if (StringUtils.isBlank(secret)) {
            throw new IllegalArgumentException("Invalid key!");
        }

        byte[] key = secret.getBytes();
        if (key.length != 16) {
            throw new IllegalArgumentException("Invalid key size!");
        }

        return new SecretKeySpec(key, algorithm);
    }

    public static String encrypt(String secretKey, String plaintext) {
        try {
            Cipher cipher = Cipher.getInstance(transformation);
            byte[] initVector = new byte[cipher.getBlockSize()];
            new SecureRandom().nextBytes(initVector);
            Key key = generateKey(secretKey, encryptionAlgorithm);
            cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(initVector));

            byte[] encoded = plaintext.getBytes(StandardCharsets.UTF_8);
            byte[] cipherText = new byte[initVector.length + cipher.getOutputSize(encoded.length)];

            for (int i = 0; i < initVector.length; i++) {
                cipherText[i] = initVector[i];
            }

            cipher.doFinal(encoded, 0, encoded.length, cipherText, initVector.length);
            return Base64.encode(cipherText);
        } catch (Throwable error) {
            throw new IllegalStateException(error.getMessage());
        }
    }

    public static String decrypt(String secretKey, String data) {
        try {
            byte[] cipherText = Base64.decode(data.getBytes(StandardCharsets.UTF_8));

            Cipher cipher = Cipher.getInstance(transformation);
            final int blockSize = cipher.getBlockSize();
            byte[] initVector = Arrays.copyOfRange(cipherText, 0, blockSize);

            IvParameterSpec ivSpec = new IvParameterSpec(initVector);
            cipher.init(Cipher.DECRYPT_MODE, generateKey(secretKey, encryptionAlgorithm), ivSpec);

            byte[] plaintext = cipher.doFinal(cipherText, blockSize, cipherText.length - blockSize);
            return new String(plaintext);
        } catch (Throwable error) {
            throw new IllegalStateException(error.getMessage());
        }
    }

    public static String sign(String data, String secret) {
        try {
            Mac mac = Mac.getInstance(signAlgorithm);
            mac.init(generateKey(secret, signAlgorithm));
            return Base64.encode(mac.doFinal(data.getBytes()));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

}