package rest.api;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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

    @Test
    public void threadSafeOnVirtualThreads() throws Exception {
        String expected = user.toString();
        long timeout = TimeUnit.MINUTES.toMillis(5);
        int requestCount = 1000;
        List<Future<Boolean>> futures = new ArrayList<>(requestCount);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < requestCount; i++) {
                futures.add(executor.submit(() -> {
                    JSONObject currentUser = new JSONObject(expected);
                    String token = SecureToken.generate(currentUser);
                    JSONObject parsedUser = SecureToken.parse(token, timeout);
                    return parsedUser != null && expected.equals(parsedUser.toString());
                }));
            }

            for (Future<Boolean> future : futures) {
                Assertions.assertTrue(future.get(30, TimeUnit.SECONDS));
            }
        }
    }

}
