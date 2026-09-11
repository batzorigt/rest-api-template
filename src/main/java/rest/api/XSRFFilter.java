package rest.api;

import io.javalin.http.Context;
import io.javalin.http.Cookie;
import io.javalin.http.ForbiddenResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum XSRFFilter {
    ;

    private static final boolean NAG_HTTPS = true;
    private static final long TIMEOUT_MILLIS = 30 * 60 * 1000;

    private static final String cookiePath = "/";
    private static final String cookieName = "xsrf-token";
    private static final String headerName = "x-xsrf-token";

    private static boolean validateToken(String headerValue, String cookieValue) {
        if (headerValue == null || cookieValue == null || !headerValue.equals(cookieValue)) {
            return false;
        }

        return XSRFToken.isValid(headerValue, TIMEOUT_MILLIS);
    }

    public static void handle(Context ctx) {
        if (NAG_HTTPS && !ctx.req().isSecure()) {
            log.warn("Using session cookies without HTTPS can expose them to session hijacking: {}", ctx.path());
        }

        switch (ctx.req().getMethod()) {
            case "GET":
                final String token = XSRFToken.generate();
                ctx.header(headerName, token);
                ctx.cookie(cookie(token));
                break;
            case "POST":
            case "PUT":
            case "DELETE":
            case "PATCH":
                if (!validateToken(ctx.req().getHeader(headerName), ctx.cookie(cookieName))) {
                    throw new ForbiddenResponse("Unauthorized Access!");
                }
                break;
            default:
                break;
        }
    }

    private static Cookie cookie(final String token) {
        return new Cookie(cookieName, token, cookiePath, -1, Server.cfg.isSecure(), true);
    }
}
