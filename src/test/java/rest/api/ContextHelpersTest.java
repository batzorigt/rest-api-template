package rest.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Locale;
import java.util.Map;

import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.javalin.http.Context;
import io.javalin.json.JavalinJackson;
import jakarta.servlet.http.HttpServletRequest;

public class ContextHelpersTest {

    @BeforeAll
    static void loadMessages() {
        I18N.load(Locale.JAPAN);
    }

    @Test
    void recordsPerPageCappedAtMaximum() {
        Assertions.assertEquals(3, ContextHelpers.capped(3));
        Assertions.assertEquals(10, ContextHelpers.capped(10));
        Assertions.assertEquals(10, ContextHelpers.capped(50));
        Assertions.assertEquals(10, ContextHelpers.capped(1_000_000));
        Assertions.assertNull(ContextHelpers.capped(null));
    }

    @Test
    void resultUsesStructuredJsonBody() {
        Context ctx = context();
        Map<String, Integer> result = Map.of("id", 1);
        ArgumentCaptor<Object> body = ArgumentCaptor.forClass(Object.class);

        ContextHelpers.result(ctx, 7, result, "json.test.message");

        verify(ctx).status(HttpStatus.OK_200);
        verify(ctx).json(body.capture());
        assertEquals(Map.of(
                "status", 7,
                "result", result,
                "msg", "message with \"quotes\""), body.getValue());
    }

    @Test
    void updateFailureUsesStructuredMessageBody() {
        Context ctx = context();
        ArgumentCaptor<Object> body = ArgumentCaptor.forClass(Object.class);

        ContextHelpers.resultOfUpdate(ctx, null);

        verify(ctx).status(HttpStatus.BAD_REQUEST_400);
        verify(ctx).json(body.capture());
        assertEquals(Map.of("msg", I18N.message("could.not.update", Locale.JAPAN)), body.getValue());
    }

    @Test
    void jsonMessageUsesConfiguredMapper() {
        Context ctx = context();
        when(ctx.jsonMapper()).thenReturn(new JavalinJackson());

        String json = ContextHelpers.jsonMessage("json.test.message", ctx);

        assertEquals("message with \"quotes\"", new JSONObject(json).getString("msg"));
    }

    private static Context context() {
        Context ctx = mock(Context.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(ctx.req()).thenReturn(request);
        when(request.getLocale()).thenReturn(Locale.JAPAN);
        return ctx;
    }
}
