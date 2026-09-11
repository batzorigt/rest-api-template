package rest.api;

import io.javalin.http.Context;
import io.javalin.http.HttpResponseException;
import io.javalin.http.HttpStatus;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class ExceptionHandlers {

    private ExceptionHandlers() {
    }

    static void exceptionHandler(Exception error, Context context) {
        log.error(error.getMessage(), error);
        context.status(HttpStatus.INTERNAL_SERVER_ERROR);
        context.contentType("text/plain").result("System Internal Error!");
    }

    static void httpResponseExceptionHandler(HttpResponseException error, Context context) {
        context.status(error.getStatus());
        if (error.getMessage() == null || !error.getMessage().stripLeading().startsWith("{")) {
            context.contentType("text/plain");
        }
        context.result(error.getMessage());
    }

}
