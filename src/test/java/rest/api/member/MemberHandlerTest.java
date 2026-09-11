package rest.api.member;

import java.util.Iterator;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;
import rest.api.Server;

public class MemberHandlerTest {

    private static Server api = new Server();
    private static String membersUrl;

    @BeforeAll
    public static void beforeAll() throws Throwable {
        api.start(0);
        membersUrl = "http://localhost:" + api.port() + "/v1/members";
    }

    @AfterAll
    public static void afterAll() {
        api.stop();
    }

    @Test
    public void createSuccess() throws UnirestException {
        JSONObject input = new JSONObject().put("name", "Batzorigt");
        HttpResponse<Member> response = Unirest.post(membersUrl).body(input)
                .asObject(Member.class);
        Member member = response.getBody();

        Assertions.assertEquals(201, response.getStatus());
        Assertions.assertNotNull(member.getId());
        Assertions.assertEquals("Batzorigt", member.getName());
        Assertions.assertNotNull(member.getCreatedAt());
        Assertions.assertNotNull(member.getUpdatedAt());
    }

    @Test
    public void createFail() throws UnirestException {
        JSONObject input = new JSONObject().put("name", "");
        HttpResponse<JsonNode> response = Unirest.post(membersUrl).body(input)
                .asJson();
        JSONObject output = response.getBody().getObject();
        JSONArray actual = (JSONArray) output.get("name");

        Assertions.assertEquals(2, actual.length());
        Assertions.assertEquals(400, response.getStatus());
        Assertions.assertTrue(contains(actual, "1 から 10 の間のサイズにしてください"));
        Assertions.assertTrue(contains(actual, "空白は許可されていません"));
    }

    @Test
    void rejectsNullPhoneList() {
        HttpResponse<String> response = Unirest.post(membersUrl)
                .header("Content-Type", "application/json")
                .body("{\"name\":\"NullPhones\",\"phones\":null}").asString();

        Assertions.assertEquals(400, response.getStatus());
    }

    @Test
    void rejectsBlankPhoneNumber() {
        HttpResponse<String> response = Unirest.post(membersUrl)
                .header("Content-Type", "application/json")
                .body("{\"name\":\"BlankPhone\",\"phones\":[\" \"]}").asString();

        Assertions.assertEquals(400, response.getStatus());
    }

    @Test
    void rejectsOversizedPhoneNumber() {
        JSONObject input = new JSONObject().put("name", "LongPhone")
                .put("phones", new JSONArray().put("x".repeat(256)));

        HttpResponse<String> response = Unirest.post(membersUrl).body(input).asString();

        Assertions.assertEquals(400, response.getStatus());
    }

    private boolean contains(JSONArray errors, String msg) {
        Iterator<Object> messages = errors.iterator();

        while (messages.hasNext()) {
            if (messages.next().equals(msg)) {
                return true;
            }
        }

        return false;
    }
}
