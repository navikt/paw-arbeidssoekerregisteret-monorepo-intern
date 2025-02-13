package no.nav.paw.bekreftelsetjeneste.metrics

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.errors.InvalidStateStoreException
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.supplyAsync
import java.util.concurrent.atomic.AtomicBoolean

private val metricLogger = LoggerFactory.getLogger("bekreftelse.tjeneste.metrics.tilstand")
fun <T1> initStateGaugeTask(
    keepGoing: AtomicBoolean,
    registry: PrometheusMeterRegistry,
    streamStateSupplier: () -> KafkaStreams.State?,
    contentSupplier: () -> Sequence<T1>,
    mapper: (T1) -> List<WithMetricsInfo>
): CompletableFuture<Unit> =
    supplyAsync {
        metricLogger.info("Starter tråd for metrics oppdateringer")
        try {
            val gauge = StateGauge(registry)
            while (keepGoing.get()) {
                try {
                    val streamState = streamStateSupplier()
                    metricLogger.trace("Stream state: $streamState")
                    if (streamState == KafkaStreams.State.RUNNING) {
                        val source = contentSupplier().flatMap(mapper)
                        gauge.update(source)
                        metricLogger.trace("Metrics oppdatert")
                        Thread.sleep(Duration.ofMinutes(10))
                    } else {
                        metricLogger.info("KafkaStreamsState={}, metrics oppdateres ikke", streamState)
                        Thread.sleep(Duration.ofMinutes(1))
                    }
                } catch (ex: InvalidStateStoreException) {
                    metricLogger.info("Metrics: state store ikke lenger gyldig", ex)
                    Thread.sleep(Duration.ofMinutes(1))
                }
            }
        } catch (ex: InterruptedException) {
            metricLogger.info("Metrics oppdateringer er avsluttet")
        }
    }.handle { _, ex ->
        if (ex != null) {
            metricLogger.error("Metrics oppdateringer er avsluttet med feil", ex)
        }
    }

