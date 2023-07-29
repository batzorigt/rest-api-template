package rest.api;

import io.javalin.http.Context;
import io.javalin.http.Cookie;
import io.javalin.http.ForbiddenResponse;
import io.javalin.util.JavalinLogger;

public enum XSRFHandler {
    ;

    private static final boolean nagHttps = true;
    private static final long timeout = 30 * 60 * 1000;

    private static final String cookiePath = "/";
    private static final String cookieName = "xsrf-token";
    private static final String headerName = "x-xsrf-token";

    private static boolean validateToken(String headerValue, String cookieValue) {
        if (headerValue == null || cookieValue == null || !headerValue.equals(cookieValue)) {
            return false;
        }

        return XSRFToken.isValid(headerValue, timeout);
    }

    public static void handle(Context ctx) {
        if (nagHttps) {
            String uri = ctx.req().getRequestURI();
            if (uri != null && !uri.startsWith("https:")) {
                JavalinLogger.warn(
                        "Using session cookies without https could make you susceptible to session hijacking: " + uri);
            }
        }

        switch (ctx.req().getMethod()) {
            case "GET":
                final String token = XSRFToken.generate();
                ctx.attribute(headerName, token);
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
        Cookie cookie = new Cookie(cookieName, token, "/", -1, API.cfg.isSecure(), 0, true, null, null, null);

        cookie.setPath(cookiePath);
        cookie.setHttpOnly(true);
        cookie.setSecure(API.cfg.isSecure());

        return cookie;
    }
}
