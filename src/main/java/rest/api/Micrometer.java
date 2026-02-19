package rest.api;

import org.jspecify.annotations.NonNull;

import io.javalin.Javalin;
import io.javalin.micrometer.MicrometerPlugin;
import io.micrometer.core.instrument.Metrics;
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

    private final PrometheusMeterRegistry registry = new PrometheusMeterRegistry(new PrometheusConfig() {

        @Override
        public String get(String key) {
            return null;
        }

        @Override
        public @NonNull String prefix() {
            return "rapit";
        }

    });

    @SuppressWarnings("resource")
    public Micrometer(Javalin javalin) {
        Metrics.addRegistry(registry);
        registry.config().commonTags("application", "rapit");

        new JvmGcMetrics().bindTo(Metrics.globalRegistry);
        new JvmHeapPressureMetrics().bindTo(Metrics.globalRegistry);
        new JvmThreadMetrics().bindTo(Metrics.globalRegistry);
        new JvmCompilationMetrics().bindTo(Metrics.globalRegistry);
        new JvmMemoryMetrics().bindTo(Metrics.globalRegistry);
        new Log4j2Metrics().bindTo(Metrics.globalRegistry);
        new UptimeMetrics().bindTo(Metrics.globalRegistry);
        new FileDescriptorMetrics().bindTo(Metrics.globalRegistry);
        new ProcessorMetrics().bindTo(Metrics.globalRegistry);
        // new DiskSpaceMetrics(new
        // File(System.getProperty("user.dir"))).bindTo(registry);

        MicrometerPlugin micrometerPlugin = new MicrometerPlugin(cfg -> cfg.registry = registry);
        javalin.unsafeConfig().registerPlugin(micrometerPlugin);
    }

    public String scrape() {
        return registry.scrape();
    }

}
