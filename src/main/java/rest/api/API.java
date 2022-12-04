package rest.api;

import static io.javalin.plugin.json.JsonMapperKt.JSON_MAPPER_KEY;

import java.util.Locale;

import org.aeonbits.owner.ConfigCache;

import io.javalin.Javalin;
import io.javalin.core.JavalinConfig;
import io.javalin.core.compression.Gzip;
import io.javalin.core.security.BasicAuthCredentials;
import io.javalin.http.Context;
import io.javalin.http.HttpResponseException;
import io.javalin.plugin.json.JsonMapper;
import io.javalin.plugin.metrics.MicrometerPlugin;
import rest.api.genre.GenreHandler;
import rest.api.member.MemberHandler;

public class API {

    public static JsonMapper jsonMapper;
    private Javalin api = Javalin.create(API::config);
    public static final Config config = ConfigCache.getOrCreate(Config.class);

    private static void config(JavalinConfig config) {
        config.enforceSsl = API.config.isSecure();
        config.autogenerateEtags = true;
        config.showJavalinBanner = false;

        config.contextPath = API.config.contextPath();
        config.asyncRequestTimeout = Long.valueOf(5000);
        config.defaultContentType = "application/json";

        config.enableDevLogging();
        config.enableCorsForOrigin(API.config.allowedOrigins());
        config.compressionStrategy(null, new Gzip());
    }

    @SuppressWarnings("resource")
    private void enableMicrometer(Javalin api) {
        api._conf.registerPlugin(new MicrometerPlugin());
        var micrometer = new Micrometer();

        api.get("/metrics", ctx -> {
            if (ctx.basicAuthCredentialsExist()) {
                BasicAuthCredentials credentials = ctx.basicAuthCredentials();

                if (API.config.monitoringUsername().equals(credentials.getUsername()) && API.config.monitoringPassword()
                        .equals(credentials.getPassword())) {
                    ctx.status(200).result(micrometer.scrape());
                }
            } else {
                ctx.status(404);
            }
        });
    }

    private void commonRequestFilter(Context ctx) {
        // TODO enable authenticator
        // AuthHandler.handle(ctx);

        if (config.xsrfProtectionEnabled()) { // TODO xsrf is for protected API
            XSRFHandler.handle(ctx);
        }
    }

    private void commonResponseFilter(Context ctx) {
        ctx.res.addHeader("Cross-Origin-Resource-Policy", "same-origin");
        ctx.res.addHeader("X-XSS-Protection", "1; mode=block");
        ctx.res.addHeader("Cache-Control", "no-store");
        ctx.res.addHeader("Content_security_policy",
                "frame-ancestors 'none'; default-src 'self' style-src 'self' 'unsafe-inline';");
        ctx.res.addHeader("Strict-Transport-Security", "max-age=63072000; includeSubDomains; preload");
        ctx.res.addHeader("X-Content-Type-Options", "nosniff");
        ctx.res.addHeader("X-Frame-Options", "DENY");
        ctx.res.addHeader("Feature-Policy", "none");
        ctx.res.addHeader("Referrer-Policy", "no-referrer");
    }

    public static void main(String[] args) throws Throwable {
        new API().start(config.portNo());
    }

    @SuppressWarnings("resource")
    public void start(int portNo) throws Throwable {
        api.before(this::commonRequestFilter);
        api.after(this::commonResponseFilter);
        api.exception(Exception.class, ExceptionHandlers::exceptionHandler);
        api.exception(HttpResponseException.class, ExceptionHandlers::httpResponseExceptionHandler);

        enableMicrometer(api);
        jsonMapper = (JsonMapper) api._conf.inner.appAttributes.get(JSON_MAPPER_KEY);

        api.events(event -> {
            event.serverStopping(() -> {
                // TODO do something here before stop
                // the code for graceful shutdown is here
            });
            event.serverStopped(() -> {
                // TODO do something here after stopped
            });
        });

        api.start(portNo);
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

        I18N.load(Locale.JAPAN);
        routes();
    }

    @SuppressWarnings("resource")
    public void stop() {
        api.stop();
    }

    private void routes() {
        GenreHandler.routes(api);
        MemberHandler.routes(api);
    }

}
