package rest.api;

import io.javalin.http.Context;
import io.javalin.http.HttpResponseException;
import io.javalin.util.JavalinLogger;
import kong.unirest.HttpStatus;

public interface ExceptionHandlers {

    static void exceptionHandler(Exception error, Context context) {
        JavalinLogger.error(error.getMessage(), error);
        context.status(HttpStatus.INTERNAL_SERVER_ERROR);
        context.result("System Internal Error!");
    }

    static void httpResponseExceptionHandler(HttpResponseException error, Context context) {
        context.status(error.getStatus());
        context.result(error.getMessage());
    }

}
