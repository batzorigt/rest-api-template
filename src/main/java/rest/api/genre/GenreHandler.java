package rest.api.genre;

import io.ebean.annotation.Transactional;
import io.javalin.Javalin;
import io.javalin.http.Context;
import rest.api.ContextHelpers;
import rest.api.PagedData;

public class GenreHandler {

    @Transactional(readOnly = true)
    static void getGenres(Context ctx) {
        Integer pageNumber = ContextHelpers.pageNumber(ctx);
        Integer pageSize = ContextHelpers.recordsPerPage(ctx);

        PagedData<Genre> result = GenreService.getGenres(pageNumber, pageSize);
        ContextHelpers.resultOfGet(ctx, result);
    }

    @SuppressWarnings("resource")
    public static void routes(Javalin app) {
        app.get("genres", GenreHandler::getGenres);
    }
}
