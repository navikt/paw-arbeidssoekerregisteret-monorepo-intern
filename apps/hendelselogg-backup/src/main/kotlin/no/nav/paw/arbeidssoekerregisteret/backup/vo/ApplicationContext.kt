package no.nav.paw.arbeidssoekerregisteret.backup.vo

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.slf4j.Logger
import java.util.concurrent.atomic.AtomicBoolean

@JvmRecord
data class ApplicationContext(
    val consumerVersion: Int,
    val logger: Logger,
    val meterRegistry: PrometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
    val shutdownCalled: AtomicBoolean = AtomicBoolean(false)
)
