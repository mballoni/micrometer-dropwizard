package br.com.poc;

import com.codahale.metrics.*;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.dropwizard.DropwizardConfig;
import io.micrometer.core.instrument.dropwizard.DropwizardMeterRegistry;
import io.micrometer.core.instrument.util.HierarchicalNameMapper;

import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

public class App {
    public static void main(String[] args) {
        MetricRegistry metricRegistry = new MetricRegistry();

        ScheduledReporter consoleReporter = ConsoleReporter.forRegistry(metricRegistry)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
        consoleReporter.start(2, TimeUnit.SECONDS);

        ScheduledReporter slf4jReporter = Slf4jReporter.forRegistry(metricRegistry)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
//        slf4jReporter.start(2, TimeUnit.SECONDS);


        ScheduledReporter csvReporter = CsvReporter.forRegistry(metricRegistry)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build(Paths.get("/tmp/micro/").toFile());
        csvReporter.start(2, TimeUnit.SECONDS);

        DropwizardConfig config = new DropwizardConfig() {
            @Override
            public String prefix() {
                return "log";
            }

            @Override
            public String get(String key) {
                return null;
            }
        };

        MeterRegistry meterRegistry = new DropwizardMeterRegistry(config, metricRegistry, HierarchicalNameMapper.DEFAULT, Clock.SYSTEM) {
            @Override
            protected Double nullGaugeValue() {
                return null;
            }
        };

        //from now on just micrometer stuff...

        new ClassLoaderMetrics().bindTo(meterRegistry);
        new JvmMemoryMetrics().bindTo(meterRegistry);
        new JvmGcMetrics().bindTo(meterRegistry);
        new ProcessorMetrics().bindTo(meterRegistry);
        new JvmThreadMetrics().bindTo(meterRegistry);

        Counter counter = meterRegistry.counter("Counter file", "gateway.entry.point", "gtw");

        Timer timer = meterRegistry.timer("timer");

        for (int i = 0; i < 10000; i++) {
            timer.record(() -> {
                for (int ii = 0; ii < 1000000; ii++) {
                    counter.increment();
                }
            });
        }
    }
}

