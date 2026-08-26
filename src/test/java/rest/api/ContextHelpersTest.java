package rest.api;

import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.javalin.json.JavalinJackson;
import rest.api.member.Member;
import rest.api.member.Phone;

public class ContextHelpersTest {

	@BeforeAll
    public static void beforeAll() throws Throwable {
        I18N.load(Locale.JAPAN);
    }

    @Test
    void setSuccessResult() {
        Member expected = new Member();
        expected.setId(1);
        expected.setName("Batzorigt");
        expected.setCreatedAt(new Date());
        expected.setUpdatedAt(new Date());

        Phone phone = new Phone();
        phone.setPhoneNo("88381882");
        expected.setPhones(List.of(phone));

        String msg = I18N.message("sucessfully.saved", Locale.JAPAN);
        JavalinJackson jsonMapper = new JavalinJackson();
		String json = jsonMapper.toJsonString(expected, expected.getClass());

        String response = String.format("{\"status\": %d, \"result\": %s, \"msg\": \"%s\"}", HttpStatus.OK_200, json,
                msg);
        JSONObject jsonObject = new JSONObject(response);
        Member actual = jsonMapper.fromJsonString(jsonObject.get("result").toString(), Member.class);

        Assertions.assertEquals(200, jsonObject.get("status"));
        Assertions.assertEquals(expected, actual);
        Assertions.assertEquals("正常に保存しました。", jsonObject.get("msg"));
    }

    @Test
    void setFailedResult() {
        String msg = I18N.message("could.not.save", Locale.JAPAN);
        String response = String.format("{\"status\": %d, \"msg\": %s}", HttpStatus.BAD_REQUEST_400, msg);
        JSONObject jsonObject = new JSONObject(response);

        Assertions.assertEquals(400, jsonObject.get("status"));
        Assertions.assertEquals("保存できませんでした！", jsonObject.get("msg"));
    }

    @Test
    void recordsPerPageCappedAtMaximum() {
        Assertions.assertEquals(3, ContextHelpers.capped(3));
        Assertions.assertEquals(10, ContextHelpers.capped(10));
        Assertions.assertEquals(10, ContextHelpers.capped(50));
        Assertions.assertEquals(10, ContextHelpers.capped(1_000_000));
        Assertions.assertNull(ContextHelpers.capped(null));
    }
}
