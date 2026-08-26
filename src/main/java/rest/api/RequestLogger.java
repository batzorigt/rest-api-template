package rest.api;

import java.util.List;
import java.util.Set;

import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class RequestLogger {

    private RequestLogger() {
    }

    private static final List<String> SAFE_HEADERS = List.of("Content-Type", "User-Agent");
    private static final Set<String> SENSITIVE_HEADERS = Set.of("cookie", "authorization");

    static void register(JavalinConfig config) {
        if (!API.cfg.requestLoggingEnabled()) {
            return;
        }

        config.requestLogger.http(RequestLogger::log);
    }

    static void log(Context ctx, Float executionTimeMillis) {
        StringBuilder line = new StringBuilder(format(
                ctx.method().name(),
                fullPath(ctx),
                ctx.status().getCode(),
                executionTimeMillis == null ? 0L : executionTimeMillis.longValue()));

        if (API.cfg.requestLoggingVerbose()) {
            appendVerbose(ctx, line);
        }

        log.info(line.toString());
    }

    static void appendVerbose(Context ctx, StringBuilder line) {
        for (String header : SAFE_HEADERS) {
            String value = ctx.header(header);

            if (value != null) {
                line.append(' ').append(headerEntry(header, value));
            }
        }

        for (String header : SENSITIVE_HEADERS) {
            String value = ctx.header(header);

            if (value != null) {
                line.append(' ').append(redacted(header, value));
            }
        }

        int contentLength = ctx.contentLength();

        if (contentLength >= 0) {
            line.append(" body=").append(contentLength).append('B');
        }
    }

    static String headerEntry(String name, String value) {
        return name + "=" + value;
    }

    static String redacted(String name, String value) {
        return name + "=REDACTED(len=" + value.length() + ")";
    }

    static String format(String method, String path, int statusCode, long durationMillis) {
        return "[http] " + method + " " + path + " -> " + statusCode + " (" + durationMillis + " ms)";
    }

    private static String fullPath(Context ctx) {
        String query = ctx.queryString();
        return query == null ? ctx.path() : ctx.path() + "?" + query;
    }
}
