package rest.api;

import java.util.List;
import java.util.Locale;
import java.util.Random;

import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import kong.unirest.json.JSONObject;
import rest.api.member.Member;
import rest.api.member.MemberService;
import rest.api.member.MemberToAdd;

public class ContextHelpersTest {

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
    void setSuccessResult() {
        MemberToAdd member = new MemberToAdd("Batzorigt", List.of("88381882"));
        Member expected = MemberService.addMember(member);

        String msg = I18N.message("sucessfully.saved", Locale.JAPAN);
        String json = API.jsonMapper.toJsonString(expected);

        String response = String.format("{\"status\": %d, \"result\": %s, \"msg\": \"%s\"}", HttpStatus.OK_200, json,
                msg);
        JSONObject jsonObject = new JSONObject(response);
        Member actual = API.jsonMapper.fromJsonString(jsonObject.get("result").toString(), Member.class);

        Assertions.assertEquals(200, jsonObject.get("status"));
        Assertions.assertEquals(expected, actual);
        Assertions.assertEquals("正常に保存しました。", jsonObject.get("msg"));
    }

    @Test
    void setFailedResult() {
        String msg = I18N.message("could.not.save", Locale.JAPAN);
        String response = String.format("{\"status\": %d, \"msg\": \"%s\"}", HttpStatus.BAD_REQUEST_400, msg);
        JSONObject jsonObject = new JSONObject(response);

        Assertions.assertEquals(400, jsonObject.get("status"));
        Assertions.assertEquals("保存できませんでした！", jsonObject.get("msg"));
    }

}
