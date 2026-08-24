package rest.api.genre;

import io.ebean.annotation.Transactional;
import io.javalin.http.Context;
import io.javalin.router.JavalinDefaultRoutingApi;
import rest.api.ContextHelpers;
import rest.api.PagedData;
import rest.api.Role;
import rest.api.Validators;

public class GenreHandler {

    @Transactional(readOnly = true)
    static void getGenres(Context ctx) {
        Integer pageNumber = ContextHelpers.pageNumber(ctx);
        Integer pageSize = ContextHelpers.recordsPerPage(ctx);

        PagedData<Genre> result = GenreService.getGenres(pageNumber, pageSize);
        ContextHelpers.resultOfGet(ctx, result);
    }

    @Transactional
    static void addGenre(Context ctx) {
        GenreToAdd input = Validators.validate(ctx, GenreToAdd.class);
        ContextHelpers.resultOfAdd(ctx, GenreService.addGenre(input));
    }

    @Transactional
    static void deleteGenre(Context ctx) {
        ContextHelpers.resultOfDelete(ctx, GenreService.deleteGenre(ContextHelpers.id(ctx)));
    }

    public static void routes(JavalinDefaultRoutingApi app) {
        app.get("genres", GenreHandler::getGenres);
        app.post("genres", GenreHandler::addGenre, Role.MANAGER);
        app.delete("genres/{id}", GenreHandler::deleteGenre, Role.ADMIN);
    }
}
