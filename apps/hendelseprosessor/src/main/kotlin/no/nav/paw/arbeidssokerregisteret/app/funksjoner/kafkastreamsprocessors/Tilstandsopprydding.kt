package no.nav.paw.arbeidssokerregisteret.app.funksjoner.kafkastreamsprocessors

import no.nav.paw.arbeidssokerregisteret.app.tilstand.InternTilstandOgHendelse
import no.nav.paw.arbeidssokerregisteret.app.tilstand.TilstandV1
import org.apache.kafka.streams.processor.Cancellable
import org.apache.kafka.streams.processor.PunctuationType
import org.apache.kafka.streams.processor.api.ProcessorContext
import org.apache.kafka.streams.state.KeyValueStore
import org.slf4j.LoggerFactory
import java.time.Duration

private val tilstandsoppryddingLogger = LoggerFactory.getLogger("Tilstandsopprydding")

fun tilstandsopprydding(
    ctx: ProcessorContext<Long, InternTilstandOgHendelse>,
    stateStore: KeyValueStore<Long, TilstandV1?>,
    interval: Duration
): Cancellable = ctx.schedule(interval, PunctuationType.WALL_CLOCK_TIME) {
    stateStore.all().use { tilstander ->
        tilstander.asSequence()
            .toList()
            .filter { it.value == null || it.value.toString() == "null"}
            .also { filtrerteTilstander ->
                val count = filtrerteTilstander.count()
                tilstandsoppryddingLogger.info("Fant {} null tilstander", count)
            }
            .forEach { tilstand ->
                tilstandsoppryddingLogger.info("Sletter tilstand med key:{} value:{}", tilstand.key, tilstand.value)
                stateStore.delete(tilstand.key)
            }
    }
}
