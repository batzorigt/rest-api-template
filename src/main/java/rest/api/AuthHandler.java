package rest.api;

import java.util.concurrent.TimeUnit;

import io.javalin.http.Context;
import io.javalin.http.UnauthorizedResponse;
import kong.unirest.json.JSONObject;

public interface AuthHandler {

    static final String secureToken = "secure-token";

    static void handle(Context ctx) {
        String token = ctx.cookie(secureToken);

        if (token == null) {
            throw new UnauthorizedResponse();
        }

        JSONObject member = SecureToken.parse(token, TimeUnit.MINUTES.toMillis(30));
        if (member == null) {
            throw new UnauthorizedResponse();
        }

        ctx.attribute(ContextAttributes.member, member);
    }

}
