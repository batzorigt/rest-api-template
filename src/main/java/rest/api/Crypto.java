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

    public static final String HMAC_SHA256 = "HmacSHA256";
    public static final String AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding";
    public static final String AES = "AES";
    public static final int GCM_IV_BYTES = 12;
    public static final int GCM_TAG_BITS = 128;
    public static final int MIN_SECRET_LENGTH = 16;
    private static final byte[] HKDF_SALT = "rest.api/Crypto/v1".getBytes(StandardCharsets.UTF_8);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final AtomicReference<KeyCache> KEY_CACHE = new AtomicReference<>();

    private Crypto() {
    }

    public static String encrypt(String secretKey, String plaintext) {
        validateSecret(secretKey);
        if (plaintext == null) {
            throw new IllegalArgumentException("Invalid plaintext!");
        }

        try {
            Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
            byte[] initVector = new byte[GCM_IV_BYTES];
            SECURE_RANDOM.nextBytes(initVector);
            cipher.init(Cipher.ENCRYPT_MODE, keysFor(secretKey).encryptionKey(),
                    new GCMParameterSpec(GCM_TAG_BITS, initVector));

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
        validateSecret(secretKey);
        if (StringUtils.isBlank(data)) {
            throw new IllegalArgumentException("Invalid encrypted payload!");
        }

        try {
            byte[] cipherText = Base64.decode(data.getBytes(StandardCharsets.UTF_8));

            if (cipherText.length <= GCM_IV_BYTES) {
                throw new IllegalArgumentException("Invalid encrypted payload!");
            }

            byte[] initVector = Arrays.copyOfRange(cipherText, 0, GCM_IV_BYTES);
            byte[] encrypted = Arrays.copyOfRange(cipherText, GCM_IV_BYTES, cipherText.length);

            Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keysFor(secretKey).encryptionKey(),
                    new GCMParameterSpec(GCM_TAG_BITS, initVector));

            byte[] plaintext = cipher.doFinal(encrypted);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException error) {
            throw new IllegalStateException("Decryption failed!", error);
        }
    }

    public static String sign(String data, String secret) {
        validateSecret(secret);
        if (data == null) {
            throw new IllegalArgumentException("Invalid data!");
        }

        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
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

        KeyCache cached = KEY_CACHE.get();
        if (cached != null && cached.secret().equals(secret)) {
            return cached;
        }

        KeyCache derived = deriveKeys(secret);
        KEY_CACHE.set(derived);
        return derived;
    }

    private static KeyCache deriveKeys(String secret) throws GeneralSecurityException {
        SecretKeySpec encryptionKey = deriveKey(secret, "enc", AES);
        SecretKeySpec signingKey = deriveKey(secret, "sig", HMAC_SHA256);
        return new KeyCache(secret, encryptionKey, signingKey);
    }

    private static SecretKeySpec deriveKey(String secret, String purpose, String algorithm)
            throws GeneralSecurityException {
        Mac mac = Mac.getInstance(HMAC_SHA256);
        mac.init(new SecretKeySpec(HKDF_SALT, HMAC_SHA256));
        byte[] prk = mac.doFinal(secret.getBytes(StandardCharsets.UTF_8));

        mac.init(new SecretKeySpec(prk, HMAC_SHA256));
        mac.update(purpose.getBytes(StandardCharsets.UTF_8));
        mac.update((byte) 0x01);
        return new SecretKeySpec(mac.doFinal(), algorithm);
    }

    private static void validateSecret(String secret) {
        if (StringUtils.isBlank(secret)) {
            throw new IllegalArgumentException("Invalid key!");
        }

        if (secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalArgumentException("Invalid key size!");
        }
    }

    private record KeyCache(String secret, SecretKeySpec encryptionKey, SecretKeySpec signingKey) {
    }

}
