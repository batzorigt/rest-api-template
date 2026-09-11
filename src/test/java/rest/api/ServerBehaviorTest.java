package rest.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;

class ServerBehaviorTest {

    private static Server api;
    private static String baseUrl;

    @BeforeAll
    static void startServer() {
        api = new Server();
        api.start(0);
        baseUrl = "http://localhost:" + api.port() + "/v1";
    }

    @AfterAll
    static void stopServer() {
        api.stop();
    }

    @Test
    void metricsChallengesMissingOrWrongBasicCredentials() {
        HttpResponse<String> missing = Unirest.get(baseUrl + "/metrics").asString();
        HttpResponse<String> wrong = Unirest.get(baseUrl + "/metrics").basicAuth("wrong", "wrong").asString();

        assertEquals(401, missing.getStatus());
        assertEquals("Basic realm=\"metrics\"", missing.getHeaders().getFirst("WWW-Authenticate"));
        assertEquals(401, wrong.getStatus());
    }

    @Test
    void metricsAcceptsConfiguredCredentials() {
        HttpResponse<String> response = Unirest.get(baseUrl + "/metrics")
                .basicAuth(Server.cfg.monitoringUsername(), Server.cfg.monitoringPassword()).asString();

        assertEquals(200, response.getStatus());
    }

    @Test
    void contentSecurityPolicySeparatesDefaultAndStyleDirectives() {
        HttpResponse<String> response = Unirest.get(baseUrl + "/genres").asString();

        assertEquals("frame-ancestors 'none'; default-src 'self'; style-src 'self' 'unsafe-inline';",
                response.getHeaders().getFirst("Content-Security-Policy"));
    }
}
