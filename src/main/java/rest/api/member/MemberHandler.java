package rest.api.member;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.post;

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

    @SuppressWarnings("resource")
    public static void routes(Javalin app) {
        // Recommended and preferred way of route definition
        app.routes(() -> {
            path("members", () -> {
                // get(MemberHandler::getAllMembers);
                post(MemberHandler::addMember);
                path("{id}", () -> {
                    get(MemberHandler::getMember);
                    // put(MemberHandler::updateMember);
                    // delete(MemberHandler::deleteMember);
                });
            });
        });
    }
}
