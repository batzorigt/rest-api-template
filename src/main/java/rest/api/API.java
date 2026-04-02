package rest.api;

import java.util.Arrays;
import java.util.Locale;

import org.aeonbits.owner.ConfigCache;

import io.javalin.Javalin;
import io.javalin.compression.CompressionStrategy;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import io.javalin.http.HttpResponseException;
import io.javalin.security.BasicAuthCredentials;
import rest.api.genre.GenreHandler;
import rest.api.member.MemberHandler;

public class API {

    private final Javalin api = Javalin.create(API::config);
    public static final Config cfg = ConfigCache.getOrCreate(Config.class);

    private static void config(JavalinConfig config) {
        config.http.generateEtags = true;
        config.http.asyncTimeout = cfg.httpAsyncTimeout();
        config.http.defaultContentType = "application/json";
        config.http.maxRequestSize = cfg.httpMaxRequestSize();

        config.concurrency.useVirtualThreads = true;
        config.http.compressionStrategy = CompressionStrategy.GZIP;
        config.startup.showJavalinBanner = false;
        config.router.contextPath = API.cfg.contextPath();

        config.bundledPlugins.enableDevLogging();
        String[] hosts = API.cfg.allowedOrigins();

        if (hosts.length > 0) {
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(it -> {
                    if (hosts.length > 1) {
                        it.allowHost(hosts[0], Arrays.copyOfRange(hosts, 1, hosts.length));
                    } else {
                        it.allowHost(hosts[0]);
                    }
                });
            });
        }

    }

    private void enableMicrometer(Javalin api) {
        var micrometer = new Micrometer(api);

        api.unsafe.routes.get("/metrics", ctx -> {
            BasicAuthCredentials credentials = ctx.basicAuthCredentials();

            if (credentials == null) {
                ctx.status(404);
            } else {
                if (API.cfg.monitoringUsername().equals(credentials.getUsername()) && API.cfg.monitoringPassword()
                        .equals(credentials.getPassword())) {
                    ctx.contentType("text/plain; version=0.0.4; charset=utf-8").result(micrometer.scrape());
                } else {
                    ctx.status(401);
                }
            }
        });
    }

    private void commonRequestFilter(Context ctx) {
        // TODO uncomment to enable authenticator
        // AuthHandler.handle(ctx);

        // TODO set true to Config#xsrfProtectionEnabled to protect from XSRF
        if (cfg.xsrfProtectionEnabled()) {
            XSRFFilter.handle(ctx);
        }
    }

    private void commonResponseFilter(Context ctx) {
        ctx.res().addHeader("Cross-Origin-Resource-Policy", "same-origin");
        ctx.res().addHeader("X-XSS-Protection", "1; mode=block");
        ctx.res().addHeader("Cache-Control", "no-store");
        ctx.res().addHeader("Content-Security-Policy",
                "frame-ancestors 'none'; default-src 'self' style-src 'self' 'unsafe-inline';");
        ctx.res().addHeader("Strict-Transport-Security", "max-age=63072000; includeSubDomains; preload");
        ctx.res().addHeader("X-Content-Type-Options", "nosniff");
        ctx.res().addHeader("X-Frame-Options", "DENY");
        ctx.res().addHeader("Feature-Policy", "none");
        ctx.res().addHeader("Referrer-Policy", "no-referrer");
    }

    public static void main(String[] args) {
        new API().start(cfg.portNo());
    }

    public void start(int portNo) {
        api.unsafe.routes.before(this::commonRequestFilter);
        api.unsafe.routes.after(this::commonResponseFilter);
        api.unsafe.routes.exception(Exception.class, ExceptionHandlers::exceptionHandler);
        api.unsafe.routes.exception(HttpResponseException.class, ExceptionHandlers::httpResponseExceptionHandler);

        enableMicrometer(api);

        api.unsafe.events.serverStopping(() -> {
            // TODO do something here before stop
            // the code for graceful shutdown is here
        });
        api.unsafe.events.serverStopped(() -> {
            // TODO do something here after stopped
        });

        I18N.load(Locale.JAPAN);
        routes();

        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
        api.start(portNo);
    }

    public void stop() {
    	// graceful shutdown
        api.stop();
    }

    private void routes() {
        GenreHandler.routes(api.unsafe.routes);
        MemberHandler.routes(api.unsafe.routes);
    }

}
