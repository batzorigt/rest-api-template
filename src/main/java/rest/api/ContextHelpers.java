package rest.api;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;

import au.com.flyingkite.mobiledetect.UAgentInfo;
import io.javalin.http.Context;

public interface ContextHelpers {

    static void result(Context ctx, int status, Object result, String msgKey, Object... args) {
        String msg = I18N.message(msgKey, ctx, args);
        String json = API.jsonMapper.toJsonString(result, result.getClass());

        // @off
        ctx.status(HttpStatus.OK_200);
        ctx.result(String.format("{\"status\": %d, \"result\": %s, \"msg\": \"%s\"}", Integer.valueOf(status), json, msg));
        // @on
    }

    static void result(Context ctx, int status, String msgKey, Object... args) {
        String msg = I18N.message(msgKey, ctx, args);

        ctx.status(HttpStatus.OK_200);
        ctx.result(String.format("{\"status\": %d, \"msg\": %s}", Integer.valueOf(status), msg));
    }

    static void resultOfSearch(Context ctx, Object result) {
        if (result == null) {
            ctx.status(HttpStatus.NO_CONTENT_204);
            ctx.result(jsonMessage("data.not.found", ctx));
        } else {
            ctx.status(HttpStatus.OK_200);
            ctx.json(result);
        }
    }

    static void resultOfGet(Context ctx, Object result) {
        if (result == null) {
            ctx.status(HttpStatus.NOT_FOUND_404);
            ctx.result(jsonMessage("data.not.found", ctx));
        } else {
            ctx.status(HttpStatus.OK_200);
            ctx.json(result);
        }
    }

    static void resultOfAdd(Context ctx, Object result) {
        if (result == null) {
            ctx.status(HttpStatus.BAD_REQUEST_400);
            ctx.result(jsonMessage("could.not.save", ctx));
        } else {
            ctx.status(HttpStatus.CREATED_201);
            ctx.json(result);
        }
    }

    static void resutlOfUpdate(Context ctx, Object result) {
        if (result == null) {
            ctx.status(HttpStatus.BAD_REQUEST_400);
            ctx.result(jsonMessage("could.not.update", ctx));
        } else {
            ctx.status(HttpStatus.OK_200);
            ctx.json(result);
        }
    }

    static void resultOfDelete(Context ctx, boolean deleted) {
        if (deleted) {
            ctx.status(HttpStatus.NO_CONTENT_204);
        } else {
            ctx.status(HttpStatus.BAD_REQUEST_400);
            ctx.result(jsonMessage("could.not.delete", ctx));
        }
    }

    static String jsonMessage(String msgKey, Context ctx, Object... params) {
        return String.format("{\"msg\": \"%s\"}", I18N.message(msgKey, ctx, params));
    }

    static Integer recordsPerPage(Context ctx) {
        if (StringUtils.isBlank(ctx.queryParam(PagedData.RECORDS_PER_PAGE))) {
            return null;
        }

        return positiveInteger(ctx, PagedData.RECORDS_PER_PAGE);
    }

    static Integer positiveInteger(Context ctx, String paramName) {
        Integer value = ctx.queryParamAsClass(paramName, int.class).get();
        return value.intValue() > 0 ? value : null;
    }

    static Integer pageNumber(Context ctx) {
        if (StringUtils.isBlank(ctx.queryParam(PagedData.PAGE_NUMBER))) {
            return null;
        }

        return positiveInteger(ctx, PagedData.PAGE_NUMBER);
    }

    static Integer id(Context ctx) {
        return ctx.pathParamAsClass("id", Integer.class).get();
    }

    static Integer integer(Context ctx, String paramName) {
        return ctx.pathParamAsClass(paramName, Integer.class).get();
    }

    static <T> List<T> arrayQueryParam(Context ctx, String key, Function<String, T> mapper) {
        List<String> queryParams = ctx.queryParams(key);
        return queryParams.stream().map(mapper).collect(Collectors.toList());
    }

    static List<Integer> integerArrayQueryParam(Context ctx, String key) {
        return arrayQueryParam(ctx, key, Integer::valueOf);
    }

    static String stringQueryParam(Context ctx, String paramName) {
        return ctx.queryParam(paramName);
    }

    static Long longQueryParam(Context ctx, String paramName) {
        if (StringUtils.isBlank(ctx.queryParam(paramName))) {
            return null;
        }

        return ctx.queryParamAsClass(paramName, Long.class).get();
    }

    static Boolean booleanQueryParam(Context ctx, String paramName) {
        if (StringUtils.isBlank(ctx.queryParam(paramName))) {
            return null;
        }

        return ctx.queryParamAsClass(paramName, Boolean.class).get();
    }

    static boolean isMobile(Context ctx) {
        return new UAgentInfo(ctx.userAgent(), ctx.header("Accept")).detectMobileLong();
    }
}
