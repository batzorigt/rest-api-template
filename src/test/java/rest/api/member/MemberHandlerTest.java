package rest.api.member;

import java.util.Iterator;
import java.util.Random;

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
import rest.api.API;

public class MemberHandlerTest {

    private static API api = new API();
    private static final int PORT_NO = 1000 + new Random().nextInt(9000);

    @BeforeAll
    public static void beforeAll() throws Throwable {
        api.start(PORT_NO);
    }

    @AfterAll
    public static void afterAll() {
        api.stop();
    }

    @Test
    public void createSuccess() throws UnirestException {
        JSONObject input = new JSONObject().put("name", "Batzorigt");
        HttpResponse<Member> response = Unirest.post("http://localhost:" + PORT_NO + "/v1/members").body(input)
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
        HttpResponse<JsonNode> response = Unirest.post("http://localhost:" + PORT_NO + "/v1/members").body(input)
                .asJson();
        JSONObject output = response.getBody().getObject();
        JSONArray actual = (JSONArray) output.get("name");

        Assertions.assertEquals(2, actual.length());
        Assertions.assertEquals(400, response.getStatus());
        Assertions.assertTrue(contains(actual, "1 から 10 の間のサイズにしてください"));
        Assertions.assertTrue(contains(actual, "空白は許可されていません"));
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
