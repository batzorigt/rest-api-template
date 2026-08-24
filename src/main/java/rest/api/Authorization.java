package rest.api;

import java.util.Set;

import org.json.JSONObject;

import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.Handler;
import io.javalin.router.Endpoint;
import io.javalin.security.Roles;
import io.javalin.security.RouteRole;

public interface Authorization {

    static Handler wrap(Endpoint endpoint) {
        Roles roles = endpoint.metadata(Roles.class);

        if (roles == null || roles.getRoles().isEmpty()) {
            return endpoint.handler;
        }

        Set<? extends RouteRole> required = roles.getRoles();

        return ctx -> manage(endpoint.handler, ctx, required);
    }

    static void manage(Handler handler, Context ctx, Set<? extends RouteRole> required) throws Exception {
        AuthFilter.handle(ctx);

        JSONObject member = ctx.attribute(ContextAttributes.member);
        Role role = Role.parse(member.optString(Role.claim));

        if (!role.satisfies(required)) {
            throw new ForbiddenResponse();
        }

        handler.handle(ctx);
    }
}
