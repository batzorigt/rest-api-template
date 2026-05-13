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

	@NonNull
    private final PrometheusMeterRegistry registry = new PrometheusMeterRegistry(new PrometheusConfig() {

		@Override
	    public String get(@NonNull String key) {
	        return null;
	    }

	    @Override
	    public @NonNull String prefix() {
	        return "rapit";
	    }

	});
    		
    @SuppressWarnings("resource")
    public Micrometer(JavalinConfig config) {
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
        // new DiskSpaceMetrics(new
        // File(System.getProperty("user.dir"))).bindTo(registry);

        MicrometerPlugin micrometerPlugin = new MicrometerPlugin(cfg -> cfg.registry = registry);
        config.registerPlugin(micrometerPlugin);
    }

    public String scrape() {
        return registry.scrape();
    }

}
