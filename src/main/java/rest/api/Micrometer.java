package rest.api;

import org.jspecify.annotations.NonNull;

import io.javalin.config.JavalinConfig;
import io.javalin.micrometer.MicrometerPlugin;
import io.micrometer.core.instrument.binder.jvm.JvmCompilationMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmHeapPressureMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.logging.Log4j2Metrics;
import io.micrometer.core.instrument.binder.system.FileDescriptorMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

public class Micrometer {

    private static final Micrometer INSTANCE = new Micrometer();

    private final PrometheusMeterRegistry registry = new PrometheusMeterRegistry(new PrometheusConfig() {

        @Override
        public String get(@NonNull String key) {
            return null;
        }

		@Override
		@SuppressWarnings("null")
        public String prefix() {
            return "rapit";
        }

    });

    @SuppressWarnings({ "resource", "null" })
	private Micrometer() {
        registry.config().commonTags("application", "rapit");

        new JvmGcMetrics().bindTo(registry);
        new JvmHeapPressureMetrics().bindTo(registry);
        new JvmThreadMetrics().bindTo(registry);
        new JvmCompilationMetrics().bindTo(registry);
        new JvmMemoryMetrics().bindTo(registry);
        new Log4j2Metrics().bindTo(registry);
        new UptimeMetrics().bindTo(registry);
        new FileDescriptorMetrics().bindTo(registry);
        new ProcessorMetrics().bindTo(registry);
        // new DiskSpaceMetrics(new File(System.getProperty("user.dir"))).bindTo(registry);
    }

    public static void register(JavalinConfig config) {
        config.registerPlugin(new MicrometerPlugin(cfg -> cfg.registry = INSTANCE.registry));
    }

    public static String scrape() {
        return INSTANCE.registry.scrape();
    }

}
