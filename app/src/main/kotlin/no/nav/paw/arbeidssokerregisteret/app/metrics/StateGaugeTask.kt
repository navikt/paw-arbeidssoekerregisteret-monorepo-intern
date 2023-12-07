package no.nav.paw.arbeidssokerregisteret.app.metrics

import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.paw.arbeidssokerregisteret.app.tilstand.Tilstand
import org.apache.kafka.streams.KafkaStreams
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.supplyAsync
import java.util.concurrent.atomic.AtomicBoolean

private val metricLogger = LoggerFactory.getLogger("tilstand_metrics")
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
                val streamState = streamStateSupplier()
                if (streamState == KafkaStreams.State.RUNNING) {
                    val source = contentSupplier().flatMap(mapper)
                    gauge.update(source)
                    metricLogger.debug("Metrics oppdatert")
                    Thread.sleep(Duration.ofMinutes(10))
                } else {
                    metricLogger.debug("KafkaStreamsState={}, metrics oppdateres ikke", streamState)
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

fun withMetricsInfoMapper(tilstand: Tilstand): List<WithMetricsInfo> =
    arbeidssoekerSituasjonsMaaler(tilstand) +
            listOfNotNull(arbeidssokerMaaler(tilstand))