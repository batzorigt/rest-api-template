package rest.api;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import kong.unirest.json.JSONObject;

public class CryptoTest {

    @Test
    void enryptAndDecryptJsonString() throws Throwable {
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
    void enryptAndDecryptRawString() throws Throwable {
        String expectedValue =
                             "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz~!@#$%^&*()_+`1234567890-={}|[]\\:”;’<>?,./";
        String encryptedValue = Crypto.encrypt(API.cfg.encryptionKey(), expectedValue);
        Assertions.assertEquals(expectedValue, Crypto.decrypt(API.cfg.encryptionKey(), encryptedValue));
    }

}
