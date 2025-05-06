package no.nav.paw.arbeidssoekerregisteret.utgang.pdl.metrics

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.utgang.pdl.kafka.serdes.HendelseState
import no.nav.paw.arbeidssoekerregisteret.utgang.pdl.kafka.serdes.UDENFINERT
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StoreQueryParameters
import org.apache.kafka.streams.state.QueryableStoreTypes
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Duration.between
import java.time.Instant
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger

private const val TILSTAND_GAUGE = "paw_arbeidssoekerregisteret_aktive_perioder_regelstatus"

class TilstandsGauge(
    private val kafkaStreams: KafkaStreams,
    private val registry: PrometheusMeterRegistry,
    private val storeName: String
) {
    private val gauges: MutableMap<Labels, Pair<Meter.Id, AtomicInteger>> = mutableMapOf()
    private val logger = LoggerFactory.getLogger("metric.gauge")

    fun run(): CompletableFuture<Void> = CompletableFuture.runAsync {
        val forsinkelseVedOk = Duration.ofMinutes(10)
        val forsinkelseVedFeil = Duration.ofSeconds(10)
        try {
            while (true) {
                if (update()) {
                    Thread.sleep(forsinkelseVedOk)
                } else {
                    Thread.sleep(forsinkelseVedFeil)
                }
            }
        } catch (e: InterruptedException) {
            logger.info("'Interrupted', avslutter metrics oppdateringer")
        } catch (e: Exception) {
            logger.error("Feil oppstod, avslutter metrics oppdateringer", e)
        }
    }

    fun update(): Boolean {
        if (kafkaStreams.state() != KafkaStreams.State.RUNNING) return false

        val tilstander: ReadOnlyKeyValueStore<UUID, HendelseState> = kafkaStreams.store(
            StoreQueryParameters.fromNameAndType(
                storeName,
                QueryableStoreTypes.keyValueStore()
            )
        )
        val timestamp = Instant.now()
        val stats = tilstander.all().use { iterator ->
            iterator.asSequence()
                .map {
                    Labels(
                        sisteTilstand = it.value.sisteEndring?.tilRegelId ?: UDENFINERT,
                        forrigeTilstand = it.value.sisteEndring?.fraRegelId ?: UDENFINERT,
                        dagerITilstand = it.value.sisteEndring?.tidspunkt?.let { endringsTidspunkt ->
                            between(endringsTidspunkt, timestamp).toDays().tilMetricVerdi()
                        } ?: "ingen_endring")
                }.groupBy { it }
                .mapValues { entry -> entry.value.size }
                .onEach { (labels, count) ->
                    gauges.computeIfAbsent(labels) { labels ->
                        val value = AtomicInteger(count)
                        Gauge.builder(
                            TILSTAND_GAUGE,
                            value
                        ) {
                            it.get().toDouble()
                        }.tags(labels.toTags())
                            .let { it.register(registry).id to value }
                    }.second.set(count)
                }.keys
        }
        gauges.toList()
            .filter { (key, _) ->  key !in stats}
            .forEach { (key, value) ->
                registry.remove(value.first)
                gauges.remove(key)
            }
        return true
    }
}

data class Labels(
    val sisteTilstand: String,
    val forrigeTilstand: String,
    val dagerITilstand: String
)

fun Labels.toTags(): Tags {
    return Tags.of(
        Tag.of("siste_tilstand", sisteTilstand),
        Tag.of("forrige_tilstand", forrigeTilstand),
        Tag.of("dager_i_tilstand", dagerITilstand)
    )
}