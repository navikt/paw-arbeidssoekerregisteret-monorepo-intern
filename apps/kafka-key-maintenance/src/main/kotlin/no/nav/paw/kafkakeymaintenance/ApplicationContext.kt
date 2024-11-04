package no.nav.paw.kafkakeymaintenance

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.slf4j.Logger
import java.util.concurrent.atomic.AtomicBoolean

@JvmRecord
data class ApplicationContext(
    val consumerVersion: Int,
    val logger: Logger,
    val meterRegistry: PrometheusMeterRegistry,
    val shutdownCalled: AtomicBoolean = AtomicBoolean(false),
)