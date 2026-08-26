package rest.api;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RequestLoggerTest {

    @Test
    void formatContainsMethodPathStatusDuration() {
        String line = RequestLogger.format("GET", "/v1/genres?page=2", 200, 12);

        Assertions.assertTrue(line.startsWith("[http]"));
        Assertions.assertTrue(line.contains("GET /v1/genres?page=2 -> 200"));
        Assertions.assertTrue(line.endsWith("(12 ms)"));
    }

    @Test
    void safeHeadersPassThrough() {
        Assertions.assertEquals("User-Agent=unirest/3", RequestLogger.headerEntry("User-Agent", "unirest/3"));
        Assertions.assertEquals("Content-Type=application/json",
                RequestLogger.headerEntry("Content-Type", "application/json"));
    }

    @Test
    void sensitiveHeadersRedactedWithLengthOnly() {
        String cookie = RequestLogger.redacted("cookie", "secure-token=abc123.signature.123");
        String auth = RequestLogger.redacted("authorization", "Basic bWljcm86bWV0ZXI=");

        Assertions.assertEquals("cookie=REDACTED(len=33)", cookie);
        Assertions.assertEquals("authorization=REDACTED(len=22)", auth);
        Assertions.assertFalse(cookie.contains("abc123"));
        Assertions.assertFalse(auth.contains("bWljcm86bWV0ZXI="));
    }
}
