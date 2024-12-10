package no.nav.paw.arbeidssokerregisteret.app.funksjoner.kafkastreamsprocessors

import no.nav.paw.arbeidssokerregisteret.app.tilstand.InternTilstandOgHendelse
import no.nav.paw.arbeidssokerregisteret.app.tilstand.TilstandV1
import org.apache.kafka.streams.processor.Cancellable
import org.apache.kafka.streams.processor.PunctuationType
import org.apache.kafka.streams.processor.api.ProcessorContext
import org.apache.kafka.streams.state.KeyValueStore
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant

private val tilstandsoppryddingLogger = LoggerFactory.getLogger("Tilstandsopprydding")

fun tilstandsopprydding(
    ctx: ProcessorContext<Long, InternTilstandOgHendelse>,
    stateStore: KeyValueStore<Long, TilstandV1?>,
    interval: Duration
): Cancellable = ctx.schedule(interval, PunctuationType.WALL_CLOCK_TIME) {
    stateStore.all().use { tilstander ->
        tilstander.asSequence()
            .filter { it.value.skalSlettes() }
            .onEach { tilstand ->
                try {
                    stateStore.delete(tilstand.key)
                    tilstandsoppryddingLogger.info("Slettet tilstand med key:{}", tilstand.key)
                } catch (e: Exception) {
                    tilstandsoppryddingLogger.error("Feil ved sletting av tilstand med key:{}", tilstand.key, e)
                }
            }
            .count().let { count ->
                tilstandsoppryddingLogger.info("Fant {} tilstander som er null eller avsluttet perioder eldre enn 6 måneder", count)
            }

    }
}

fun TilstandV1?.skalSlettes(): Boolean =
   (this == null || this.toString() == "null")
           || (this.gjeldenePeriode?.avsluttet != null && this.gjeldenePeriode.avsluttet.tidspunkt.erEldreEnn6maaneder())


fun Instant.erEldreEnn6maaneder(): Boolean = isBefore(Instant.now().minus(Duration.ofDays(180)))

