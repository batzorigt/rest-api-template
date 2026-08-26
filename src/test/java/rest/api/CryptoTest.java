package rest.api;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CryptoTest {

    @Test
    void enryptAndDecryptJsonString() {
        JSONObject user = new JSONObject();
        user.put("id", 1);
        user.put("name", "name");
        user.put("phoneNo", "12345678");
        user.put("emailAddress", "email@address");

        String expectedValue = user.toString();
        String encryptedValue = Crypto.encrypt(API.cfg.encryptionKey(), expectedValue);
        Assertions.assertEquals(expectedValue, Crypto.decrypt(API.cfg.encryptionKey(), encryptedValue));
    }

    @Test
    void enryptAndDecryptRawString() {
        String expectedValue = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz~!@#$%^&*()_+`1234567890-={}|[]\\:\";'<>?,./";
        String encryptedValue = Crypto.encrypt(API.cfg.encryptionKey(), expectedValue);
        Assertions.assertEquals(expectedValue, Crypto.decrypt(API.cfg.encryptionKey(), encryptedValue));
    }

    @Test
    void encryptUsesRandomInitVector() {
        String payload = "same-input";
        String encryptedValueOne = Crypto.encrypt(API.cfg.encryptionKey(), payload);
        String encryptedValueTwo = Crypto.encrypt(API.cfg.encryptionKey(), payload);

        Assertions.assertNotEquals(encryptedValueOne, encryptedValueTwo);
    }

    @Test
    void decryptFailsWhenPayloadTampered() {
        String payload = "payload";
        String encryptedValue = Crypto.encrypt(API.cfg.encryptionKey(), payload);
        byte[] tampered = Base64.decode(encryptedValue.getBytes(StandardCharsets.UTF_8));
        tampered[tampered.length - 1] ^= 0x01;

        Assertions.assertThrows(IllegalStateException.class,
                () -> Crypto.decrypt(API.cfg.encryptionKey(), Base64.encode(tampered)));
    }

    @Test
    void rejectsInvalidSecrets() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Crypto.encrypt(null, "payload"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Crypto.encrypt("", "payload"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Crypto.encrypt("short-key", "payload"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Crypto.sign("data", "short-key"));
    }

    @Test
    void rejectsInvalidInputs() {
        String key = API.cfg.encryptionKey();

        Assertions.assertThrows(IllegalArgumentException.class, () -> Crypto.encrypt(key, null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Crypto.decrypt(key, " "));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Crypto.sign(null, key));
    }

    @Test
    void decryptFailsWithWrongKey() {
        String encrypted = Crypto.encrypt(API.cfg.encryptionKey(), "secret payload");
        String otherKey = "another-secret-key-16";

        Assertions.assertThrows(IllegalStateException.class, () -> Crypto.decrypt(otherKey, encrypted));
    }

    @Test
    void constantTimeEqualsNullSafety() {
        Assertions.assertFalse(Crypto.constantTimeEquals(null, "value"));
        Assertions.assertFalse(Crypto.constantTimeEquals("value", null));
        Assertions.assertFalse(Crypto.constantTimeEquals("value-a", "value-b"));
        Assertions.assertTrue(Crypto.constantTimeEquals("value", "value"));
    }

    @Test
    void threadSafeOnVirtualThreads() throws Exception {
        String secretKey = API.cfg.encryptionKey();
        int requestCount = 2000;
        List<Future<Boolean>> futures = new ArrayList<>(requestCount);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < requestCount; i++) {
                final int index = i;
                futures.add(executor.submit(() -> {
                    String payload = "{\"request\":" + index + ",\"value\":\"payload-" + index + "\"}";
                    String encrypted = Crypto.encrypt(secretKey, payload);
                    String decrypted = Crypto.decrypt(secretKey, encrypted);

                    if (!payload.equals(decrypted)) {
                        return false;
                    }

                    String signature = Crypto.sign(encrypted, secretKey);
                    String expected = Crypto.sign(encrypted, secretKey);
                    return Crypto.constantTimeEquals(signature, expected);
                }));
            }

            for (Future<Boolean> future : futures) {
                Assertions.assertTrue(future.get(30, TimeUnit.SECONDS));
            }
        }
    }

}
