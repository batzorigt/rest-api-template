package rest.api.member;

import io.ebean.annotation.Transactional;
import io.javalin.Javalin;
import io.javalin.http.Context;
import rest.api.ContextHelpers;
import rest.api.Validators;

public class MemberHandler {

    @Transactional(readOnly = true)
    static void getMember(Context ctx) {
        var member = MemberService.getMember(ContextHelpers.id(ctx));
        ContextHelpers.resultOfGet(ctx, member);
    }

    @Transactional
    static void addMember(Context ctx) {
        MemberToAdd input = Validators.validate(ctx, MemberToAdd.class);
        var addedMember = MemberService.addMember(input);
        ContextHelpers.resultOfAdd(ctx, addedMember);
    }

    public static void routes(Javalin app) {
        // Direct route registration (Javalin 6)
        app.post("members", MemberHandler::addMember);
        app.get("members/{id}", MemberHandler::getMember);
    }
}
