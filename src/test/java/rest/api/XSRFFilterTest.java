package rest.api;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.javalin.http.Context;
import io.javalin.http.Cookie;
import io.javalin.http.ForbiddenResponse;
import jakarta.servlet.http.HttpServletRequest;

class XSRFFilterTest {

    @Test
    void getReturnsAnHttpOnlyCookieAndMatchingResponseHeader() {
        Context ctx = mock(Context.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(ctx.req()).thenReturn(request);
        when(request.isSecure()).thenReturn(true);
        when(request.getMethod()).thenReturn("GET");

        XSRFFilter.handle(ctx);

        ArgumentCaptor<String> header = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Cookie> cookie = ArgumentCaptor.forClass(Cookie.class);
        verify(ctx).header(eq("x-xsrf-token"), header.capture());
        verify(ctx).cookie(cookie.capture());
        assertTrue(XSRFToken.isValid(header.getValue(), 30 * 60 * 1000));
        assertTrue(header.getValue().equals(cookie.getValue().getValue()));
        assertTrue(cookie.getValue().isHttpOnly());
    }

    @Test
    void mutationRequiresMatchingValidHeaderAndCookie() {
        Context ctx = mock(Context.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        String token = XSRFToken.generate();
        when(ctx.req()).thenReturn(request);
        when(request.isSecure()).thenReturn(true);
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("x-xsrf-token")).thenReturn(token);
        when(ctx.cookie("xsrf-token")).thenReturn(token);

        XSRFFilter.handle(ctx);

        when(request.getHeader("x-xsrf-token")).thenReturn("wrong");
        assertThrows(ForbiddenResponse.class, () -> XSRFFilter.handle(ctx));
    }
}
