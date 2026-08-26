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
        config.http.compressionStrategy = CompressionStrategy.GZIP;

        config.concurrency.useVirtualThreads = true;
        config.startup.showJavalinBanner = false;
        config.router.contextPath = API.cfg.contextPath();

        if (Config.LOCAL_ENVIRONMENT.equalsIgnoreCase(cfg.environment())) {
            config.bundledPlugins.enableDevLogging();
        }
        
        RequestLogger.register(config);
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

        Micrometer.register(config);

        config.router.handlerWrapper(Authorization::wrap);

        config.routes.before(API::commonRequestFilter);
        config.routes.after(API::commonResponseFilter);
        config.routes.exception(Exception.class, ExceptionHandlers::exceptionHandler);
        config.routes.exception(HttpResponseException.class, ExceptionHandlers::httpResponseExceptionHandler);
        config.routes.get("/metrics", ctx -> {
            BasicAuthCredentials credentials = ctx.basicAuthCredentials();

            if (credentials == null) {
                ctx.status(404);
            } else {
                if (API.cfg.monitoringUsername().equals(credentials.getUsername()) && API.cfg.monitoringPassword()
                        .equals(credentials.getPassword())) {
                    ctx.contentType("text/plain; version=0.0.4; charset=utf-8").result(Micrometer.scrape());
                } else {
                    ctx.status(401);
                }
            }
        });
        
        routes(config);

        config.events.serverStopping(() -> {
            // TODO do something here before stop
            // the code for graceful shutdown is here
        });
        config.events.serverStopped(() -> {
            // TODO do something here after stopped
        });
    }

	private static void routes(JavalinConfig config) {
		GenreHandler.routes(config.routes);
        MemberHandler.routes(config.routes);
	}

    private static void commonRequestFilter(Context ctx) {
        // TODO set true to Config#xsrfProtectionEnabled to protect from XSRF
        if (cfg.xsrfProtectionEnabled()) {
            XSRFFilter.handle(ctx);
        }
    }

    private static void commonResponseFilter(Context ctx) {
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
        StartupGuard.requireSafeProductionConfig(cfg.environment(), cfg.encryptionKey());
        I18N.load(Locale.JAPAN);
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
        api.start(portNo);
    }

    public void stop() {
    	// graceful shutdown
        api.stop();
    }

}
