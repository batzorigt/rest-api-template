package rest.api;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import kong.unirest.json.JSONObject;

public class SecureTokenTest {

    private static JSONObject user = new JSONObject();

    @BeforeAll
    public static void init() throws Throwable {
        user.put("id", "1");
        user.put("name", "name");
        user.put("phoneNo", "12345678");
        user.put("emailAddress", "email@address");
    }

    @Test
    public void success() {
        String accessToken = SecureToken.generate(user);
        Assertions.assertEquals(user.toString(), SecureToken.parse(accessToken, TimeUnit.MINUTES.toMillis(5))
                .toString());
    }

    @Test
    public void failWhenTokenExpired() throws InterruptedException {
        String accessToken = SecureToken.generate(user);
        Thread.sleep(2);
        Assertions.assertNull(SecureToken.parse(accessToken, 1));
    }

    @Test
    public void failWhenInvalidTokenReceived() {
        Assertions.assertNull(SecureToken.parse("invalidToken", 100));
    }

}
