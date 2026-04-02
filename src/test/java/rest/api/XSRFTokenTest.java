package rest.api;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class XSRFTokenTest {

    @Test
    public void success() {
        String xsrfToken = XSRFToken.generate();
        Assertions.assertTrue(XSRFToken.isValid(xsrfToken, TimeUnit.MINUTES.toMillis(5)));
    }

    @Test
    public void successForAllPossibleCharacters() {
        String xsrfToken = XSRFToken.generate(
                "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz~!@#$%^&*()_+`1234567890-={}|[]\\:\";'<>?,./");
        Assertions.assertTrue(XSRFToken.isValid(xsrfToken, TimeUnit.MINUTES.toMillis(5)));
    }

    @Test
    public void failWhenTokenExpired() throws InterruptedException {
        String xsrfToken = XSRFToken.generate();
        Thread.sleep(2);
        Assertions.assertFalse(XSRFToken.isValid(xsrfToken, 1));
    }

    @Test
    public void failWhenInvalidTokenReceived() {
        Assertions.assertFalse(XSRFToken.isValid("invalidToken", 100));
    }

    @Test
    public void threadSafeOnVirtualThreads() throws Exception {
        int requestCount = 2000;
        long timeout = TimeUnit.MINUTES.toMillis(5);
        List<Future<Boolean>> futures = new ArrayList<>(requestCount);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < requestCount; i++) {
                futures.add(executor.submit(() -> {
                    String xsrfToken = XSRFToken.generate();
                    return XSRFToken.isValid(xsrfToken, timeout);
                }));
            }

            for (Future<Boolean> future : futures) {
                Assertions.assertTrue(future.get(30, TimeUnit.SECONDS));
            }
        }
    }

}
