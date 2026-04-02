package rest.api;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang3.StringUtils;

public final class Crypto {

    public static final String signAlgorithm = "HmacSHA256";
    public static final String transformation = "AES/GCM/NoPadding";
    public static final String encryptionAlgorithm = "AES";
    public static final int gcmInitVectorSize = 12;
    public static final int gcmTagLengthBits = 128;
    public static final int minimumSecretLength = 16;
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final AtomicReference<KeyCache> keyCache = new AtomicReference<>();

    private Crypto() {
    }

    public static String encrypt(String secretKey, String plaintext) {
        if (plaintext == null) {
            throw new IllegalArgumentException("Invalid plaintext!");
        }

        try {
            Cipher cipher = Cipher.getInstance(transformation);
            byte[] initVector = new byte[gcmInitVectorSize];
            secureRandom.nextBytes(initVector);
            cipher.init(Cipher.ENCRYPT_MODE, keysFor(secretKey).encryptionKey(),
                    new GCMParameterSpec(gcmTagLengthBits, initVector));

            byte[] encoded = plaintext.getBytes(StandardCharsets.UTF_8);
            byte[] encrypted = cipher.doFinal(encoded);
            byte[] cipherText = new byte[initVector.length + encrypted.length];
            System.arraycopy(initVector, 0, cipherText, 0, initVector.length);
            System.arraycopy(encrypted, 0, cipherText, initVector.length, encrypted.length);
            return Base64.encode(cipherText);
        } catch (GeneralSecurityException | IllegalArgumentException error) {
            throw new IllegalStateException("Encryption failed!", error);
        }
    }

    public static String decrypt(String secretKey, String data) {
        if (StringUtils.isBlank(data)) {
            throw new IllegalArgumentException("Invalid encrypted payload!");
        }

        try {
            byte[] cipherText = Base64.decode(data.getBytes(StandardCharsets.UTF_8));

            if (cipherText.length <= gcmInitVectorSize) {
                throw new IllegalArgumentException("Invalid encrypted payload!");
            }

            byte[] initVector = Arrays.copyOfRange(cipherText, 0, gcmInitVectorSize);
            byte[] encrypted = Arrays.copyOfRange(cipherText, gcmInitVectorSize, cipherText.length);

            Cipher cipher = Cipher.getInstance(transformation);
            cipher.init(Cipher.DECRYPT_MODE, keysFor(secretKey).encryptionKey(),
                    new GCMParameterSpec(gcmTagLengthBits, initVector));

            byte[] plaintext = cipher.doFinal(encrypted);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException error) {
            throw new IllegalStateException("Decryption failed!", error);
        }
    }

    public static String sign(String data, String secret) {
        if (data == null) {
            throw new IllegalArgumentException("Invalid data!");
        }

        try {
            Mac mac = Mac.getInstance(signAlgorithm);
            mac.init(keysFor(secret).signingKey());
            return Base64.encode(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Signing failed!", e);
        }
    }

    public static boolean constantTimeEquals(String valueA, String valueB) {
        if (valueA == null || valueB == null) {
            return false;
        }

        return MessageDigest.isEqual(valueA.getBytes(StandardCharsets.UTF_8), valueB.getBytes(StandardCharsets.UTF_8));
    }

    private static KeyCache keysFor(String secret) throws GeneralSecurityException {
        validateSecret(secret);

        KeyCache cached = keyCache.get();
        if (cached != null && cached.secret().equals(secret)) {
            return cached;
        }

        KeyCache derived = deriveKeys(secret);
        keyCache.set(derived);
        return derived;
    }

    private static KeyCache deriveKeys(String secret) throws GeneralSecurityException {
        SecretKeySpec encryptionKey = deriveKey(secret, "enc", encryptionAlgorithm);
        SecretKeySpec signingKey = deriveKey(secret, "sig", signAlgorithm);
        return new KeyCache(secret, encryptionKey, signingKey);
    }

    private static SecretKeySpec deriveKey(String secret, String purpose, String algorithm)
            throws GeneralSecurityException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(secret.getBytes(StandardCharsets.UTF_8));
        digest.update((byte) ':');
        digest.update(purpose.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(digest.digest(), algorithm);
    }

    private static void validateSecret(String secret) {
        if (StringUtils.isBlank(secret)) {
            throw new IllegalArgumentException("Invalid key!");
        }

        if (secret.length() < minimumSecretLength) {
            throw new IllegalArgumentException("Invalid key size!");
        }
    }

    private record KeyCache(String secret, SecretKeySpec encryptionKey, SecretKeySpec signingKey) {
    }

}
