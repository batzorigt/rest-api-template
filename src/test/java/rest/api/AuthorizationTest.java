package rest.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.ebean.DB;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;
import rest.api.genre.DGenre;
import rest.api.genre.query.QDGenre;
import rest.api.member.DMember;
import rest.api.member.query.QDMember;

public class AuthorizationTest {

    private static API api = new API();
    private static final int PORT_NO = 1000 + new Random().nextInt(9000);
    private static final String BASE_URL = String.format("http://localhost:%d/v1", PORT_NO);

    @BeforeAll
    public static void beforeAll() throws Throwable {
        api.start(PORT_NO);
    }

    @AfterAll
    public static void afterAll() {
        api.stop();
    }

    @BeforeEach
    public void before() {
        new QDGenre().delete();
        new QDMember().delete();
    }

    @Test
    void anonymousIsUnauthorizedOnProtectedRoute() {
        HttpResponse<String> response = Unirest.get(BASE_URL + "/members/1").asString();

        assertEquals(401, response.getStatus());
    }

    @Test
    void invalidTokenIsUnauthorized() {
        HttpResponse<String> response = Unirest.get(BASE_URL + "/members/1")
                .cookie(AuthFilter.secureToken, "not-a-valid-token").asString();

        assertEquals(401, response.getStatus());
    }

    @Test
    void expiredTokenIsUnauthorized() {
        String payload = Crypto.encrypt(API.cfg.encryptionKey(), sessionWithRole(Role.USER.name()).toString());
        long expiredTimestamp = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(31);
        String timestampedPayload = payload + "." + expiredTimestamp;
        String expiredToken = timestampedPayload + "." + XSRFToken.sign(timestampedPayload);

        HttpResponse<String> response = Unirest.get(BASE_URL + "/members/1")
                .cookie(AuthFilter.secureToken, expiredToken).asString();

        assertEquals(401, response.getStatus());
    }

    @Test
    void loggedUserCanReadMember() {
        int memberId = insertMember("Batzorigt");

        HttpResponse<JsonNode> response = Unirest.get(BASE_URL + "/members/" + memberId)
                .cookie(AuthFilter.secureToken, tokenForRole(Role.USER.name())).asJson();

        assertEquals(200, response.getStatus());
        assertEquals("Batzorigt", response.getBody().getObject().getString("name"));
    }

    @Test
    void tokenWithoutRoleClaimDefaultsToUser() {
        int memberId = insertMember("NoRole");
        org.json.JSONObject claims = new org.json.JSONObject().put("id", memberId).put("name", "NoRole");
        String token = SecureToken.generate(claims);

        HttpResponse<JsonNode> readResponse = Unirest.get(BASE_URL + "/members/" + memberId)
                .cookie(AuthFilter.secureToken, token).asJson();
        HttpResponse<String> writeResponse = addGenre(token);

        assertEquals(200, readResponse.getStatus());
        assertEquals("NoRole", readResponse.getBody().getObject().getString("name"));
        assertEquals(403, writeResponse.getStatus());
    }

    @Test
    void userCannotCreateGenre() {
        HttpResponse<String> response = addGenre(tokenForRole(Role.USER.name()));

        assertEquals(403, response.getStatus());
        assertEquals(0, new QDGenre().findCount());
    }

    @Test
    void managerCanCreateGenre() {
        HttpResponse<JsonNode> response = Unirest.post(BASE_URL + "/genres")
                .header("Content-Type", "application/json").body("{\"name\":\"Action\",\"orderNumber\":7}")
                .cookie(AuthFilter.secureToken, tokenForRole(Role.MANAGER.name())).asJson();

        assertEquals(201, response.getStatus());
        assertEquals("Action", response.getBody().getObject().getString("name"));
        assertEquals(7, response.getBody().getObject().getInt("orderNumber"));
        assertEquals(1, new QDGenre().findCount());
    }

    @Test
    void managerCannotDeleteGenre() {
        int genreId = insertGenre("Drama");

        HttpResponse<String> response = deleteGenre(genreId, Role.MANAGER.name());

        assertEquals(403, response.getStatus());
        assertNotNull(DB.find(DGenre.class, genreId));
    }

    @Test
    void adminCanDeleteGenre() {
        int genreId = insertGenre("Comedy");

        HttpResponse<String> response = deleteGenre(genreId, Role.ADMIN.name());

        assertEquals(204, response.getStatus());
        assertNull(DB.find(DGenre.class, genreId));
    }

    @Test
    void anonymousCanStillUsePublicRoutes() {
        HttpResponse<String> genres = Unirest.get(BASE_URL + "/genres").asString();
        HttpResponse<String> register = Unirest.post(BASE_URL + "/members")
                .header("Content-Type", "application/json").body("{\"name\":\"Anon\"}").asString();

        assertNotEquals(401, genres.getStatus());
        assertNotEquals(403, genres.getStatus());
        assertEquals(201, register.getStatus());
    }

    private static HttpResponse<String> addGenre(String token) {
        return Unirest.post(BASE_URL + "/genres").header("Content-Type", "application/json")
                .body("{\"name\":\"Action\"}").cookie(AuthFilter.secureToken, token).asString();
    }

    private static HttpResponse<String> deleteGenre(int genreId, String role) {
        return Unirest.delete(BASE_URL + "/genres/" + genreId)
                .cookie(AuthFilter.secureToken, tokenForRole(role)).asString();
    }

    private static org.json.JSONObject sessionWithRole(String role) {
        return new org.json.JSONObject().put("id", 1).put("name", "tester").put(Role.claim, role);
    }

    private static String tokenForRole(String role) {
        return SecureToken.generate(sessionWithRole(role));
    }

    private static int insertMember(String name) {
        DMember member = new DMember(name);
        member.save();
        return member.getId();
    }

    private static int insertGenre(String name) {
        DGenre genre = new DGenre();
        genre.setName(name);
        genre.setOrderNumber(1);
        genre.save();
        return genre.getId();
    }
}
