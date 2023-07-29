package rest.api;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

import io.javalin.http.Context;

public abstract class I18N {

    private static Map<Locale, ResourceBundle> resources = new ConcurrentHashMap<>();

    protected static void load(Locale... locales) {
        load("i18n", locales);
    }

    protected static void load(String baseName, Locale... locales) {
        for (Locale locale : locales) {
            resources.put(locale, ResourceBundle.getBundle(baseName, locale));
        }
    }

    public static String fieldName(String key, Context ctx, Object... args) {
        return message(key + ".field.caption", ctx, args);
    }

    public static String message(String key, Context ctx, Object... args) {
        return message(key, ctx.req().getLocale(), args);
    }

    public static String message(String key, Locale locale, Object... args) {
        ResourceBundle resource = resources.get(locale);

        if (resource == null && !Locale.JAPAN.equals(locale)) {
            resource = resources.get(Locale.JAPAN);
        }

        if (resource == null && !Locale.JAPAN.equals(locale)) {
            resource = resources.get(Locale.getDefault());
        }

        return MessageFormat.format(resource.getString(key).trim(), args);
    }

}
