package no.nav.paw.metrics.plugin

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

private val defaultMeterBinders = listOf(
    JvmMemoryMetrics(),
    JvmGcMetrics(),
    ProcessorMetrics()
)

fun Application.installMetricsPlugin(
    meterRegistry: MeterRegistry,
    additionalMeterBinders: List<MeterBinder> = emptyList(),
    distributionStatisticConfig: DistributionStatisticConfig = DistributionStatisticConfig.NONE
) {
    install(MicrometerMetrics) {
        this.registry = meterRegistry
        this.meterBinders = defaultMeterBinders + additionalMeterBinders
        this.distributionStatisticConfig = distributionStatisticConfig
    }
}

fun Application.installWebAppMetricsPlugin(
    meterRegistry: MeterRegistry,
    additionalMeterBinders: List<MeterBinder> = emptyList(),
    enablePercentilesHistogram: Boolean = true,
    minimumExpectedValue: Duration = Duration.ofMillis(10),
    maximumExpectedValue: Duration = Duration.ofSeconds(1),
    serviceLevelObjectives: List<Duration> = listOf(
        Duration.ofMillis(50),
        Duration.ofMillis(100),
        Duration.ofMillis(300),
        Duration.ofMillis(500)
    )
) {
    val distributionStatisticConfig =
        DistributionStatisticConfig.builder()
            .percentilesHistogram(enablePercentilesHistogram)
            .minimumExpectedValue(minimumExpectedValue.toNanos().toDouble())
            .maximumExpectedValue(maximumExpectedValue.toNanos().toDouble())
            .serviceLevelObjectives(*serviceLevelObjectives.map { it.toNanos().toDouble() }.toDoubleArray())
            .build()
    installMetricsPlugin(meterRegistry, additionalMeterBinders, distributionStatisticConfig)
}
