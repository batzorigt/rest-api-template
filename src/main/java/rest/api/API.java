package rest.api;

import java.util.Arrays;
import java.util.Locale;

import org.aeonbits.owner.ConfigCache;

import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import io.javalin.http.HttpResponseException;
import io.javalin.json.JsonMapper;
import io.javalin.security.BasicAuthCredentials;
import io.prometheus.client.exporter.common.TextFormat;
import rest.api.genre.GenreHandler;
import rest.api.member.MemberHandler;

public class API {

    public static JsonMapper jsonMapper;
    private final Javalin api = Javalin.create(API::config);
    public static final Config cfg = ConfigCache.getOrCreate(Config.class);

    private static void config(JavalinConfig config) {
    	config.http.generateEtags = true;
    	config.http.asyncTimeout = cfg.httpAsyncTimeout();
    	config.http.defaultContentType = "application/json";
    	config.http.maxRequestSize = cfg.httpMaxRequestSize();

    	config.compression.gzipOnly();
    	config.showJavalinBanner = false;
        config.routing.contextPath = API.cfg.contextPath();

        config.plugins.enableDevLogging(); 
        String[] hosts = API.cfg.allowedOrigins();
        
        if(hosts.length > 0) {
            config.plugins.enableCors(cors -> {
                cors.add(it -> {
                    if(hosts.length > 1) {
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

        api.get("/metrics", ctx -> {
        	BasicAuthCredentials credentials = ctx.basicAuthCredentials();
        	
            if (credentials != null) {
                if (API.cfg.monitoringUsername().equals(credentials.getUsername()) && API.cfg.monitoringPassword()
                        .equals(credentials.getPassword())) {
                    ctx.contentType(TextFormat.CONTENT_TYPE_004).result(micrometer.scrape());
                }
            } else {
                ctx.status(404);
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
        ctx.res().addHeader("Content_security_policy",
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

    public void start(int portNo)  {
        api.before(this::commonRequestFilter);
        api.after(this::commonResponseFilter);
        api.exception(Exception.class, ExceptionHandlers::exceptionHandler);
        api.exception(HttpResponseException.class, ExceptionHandlers::httpResponseExceptionHandler);

        enableMicrometer(api);
        jsonMapper = (JsonMapper) api.cfg.pvt.appAttributes.get("javalin-json-mapper");

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

    public void stop() {
        api.stop();
    }

    private void routes() {
        GenreHandler.routes(api);
        MemberHandler.routes(api);
    }

}
