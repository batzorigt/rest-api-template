package rest.api;

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
                "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz~!@#$%^&*()_+`1234567890-={}|[]\\:”;’<>?,./");
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
        XSRFToken.isValid("invalidToken", 100);
    }

}
