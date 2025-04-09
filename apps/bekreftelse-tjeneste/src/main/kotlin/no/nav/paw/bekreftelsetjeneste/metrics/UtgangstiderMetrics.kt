package no.nav.paw.bekreftelsetjeneste.metrics

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.bekreftelsetjeneste.logger
import no.nav.paw.bekreftelsetjeneste.tilstand.BekreftelseTilstand
import no.nav.paw.bekreftelsetjeneste.tilstand.VenterPaaSvar
import no.nav.paw.bekreftelsetjeneste.tilstand.VenterSvar
import no.nav.paw.bekreftelsetjeneste.tilstand.sisteTilstand
import no.nav.paw.bekreftelsetjeneste.tilstand.tilstand
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore
import java.time.Duration
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.runAsync
import java.util.concurrent.atomic.AtomicLong

private const val exit = 1
private val terminalStates = hashSetOf(
    KafkaStreams.State.ERROR,
    KafkaStreams.State.PENDING_SHUTDOWN
)

private val nesteUtloepTimestamp = AtomicLong(0L)
private val antallVedNesteUtloep = AtomicLong(0L)

fun init(
    registry: PrometheusMeterRegistry,
    kafkaStreams: KafkaStreams,
    storeName: String,
    graceperiode: Duration
): CompletableFuture<Void> {
    registry.gauge("bekreftelse_utloep_timestamp", nesteUtloepTimestamp)
    registry.gauge("bekreftelse_utloep_antall", antallVedNesteUtloep)
    return runAsync {
        while(kafkaStreams.ventPaaKlar() == 0) {
            try {
                val store: ReadOnlyKeyValueStore<UUID, BekreftelseTilstand> = kafkaStreams.keyValueStateStore(storeName)
                store.all().use { tilstander ->
                    tilstander.asSequence()
                        .mapNotNull { tilstand ->
                            tilstand.value.bekreftelser
                                .firstOrNull { it.sisteTilstand() is VenterPaaSvar }
                                ?.tilstand<VenterSvar>()
                                ?.let { it.timestamp + graceperiode }
                        }
                        .groupBy { it }
                        .mapValues { it.value.size }
                        .onEach { logger.info("På denne noden klokken {}, går fristen ut for {} personer", it.key, it.value) }
                        .minByOrNull { it.key }
                        .also { neste ->
                            val ts = neste?.key?.toEpochMilli() ?: 0L
                            val antall = neste?.value ?: 0
                            nesteUtloepTimestamp.set(ts)
                            antallVedNesteUtloep.set(antall.toLong())
                        }
                }
                Thread.sleep(Duration.ofMinutes(10))
            } catch (ex: Exception) {
                logger.warn("Feil ved generering av utgangsstatistikk", ex)
                Thread.sleep(Duration.ofMinutes(1))
            }
        }
    }
}

fun KafkaStreams.ventPaaKlar(): Int {
    var state = state()
    while(state != KafkaStreams.State.RUNNING) {
        if (state in terminalStates) return exit
        Thread.sleep(1000)
        state = state()
    }
    return 0
}