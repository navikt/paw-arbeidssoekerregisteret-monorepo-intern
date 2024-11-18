package no.nav.paw.kafkakeygenerator.plugin

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.MeterBinder
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig
import java.time.Duration

fun Application.configureMetrics(
    meterRegistry: MeterRegistry,
    extraMeterBinders: List<MeterBinder>
) {
    install(MicrometerMetrics) {
        this.registry = meterRegistry
        this.meterBinders = listOf(
            JvmGcMetrics(),
            JvmMemoryMetrics(),
            ProcessorMetrics()
        ) + extraMeterBinders
        this.distributionStatisticConfig =
            DistributionStatisticConfig.builder()
                .percentilesHistogram(true)
                .maximumExpectedValue(Duration.ofMillis(750).toNanos().toDouble())
                .minimumExpectedValue(Duration.ofMillis(20).toNanos().toDouble())
                .serviceLevelObjectives(
                    Duration.ofMillis(100).toNanos().toDouble(),
                    Duration.ofMillis(200).toNanos().toDouble()
                )
                .build()
    }
}
