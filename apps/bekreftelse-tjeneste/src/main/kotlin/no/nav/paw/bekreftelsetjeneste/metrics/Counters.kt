package no.nav.paw.bekreftelsetjeneste.metrics

import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import java.time.Duration
import java.time.Instant


private const val bekreftelseMottattCounter = "arbeidssoekerregisteret_bekreftelse_mottatt_v1"
private const val LOESNING = "loesning"
private const val FORTSETTE = "vil_fortsette"
private const val HAR_JOBBET = "har_jobbet"
private const val PERIODE_FUNNET = "periode_funnet"
private const val HAR_ANSVAR = "har_ansvar"

fun PrometheusMeterRegistry.tellBekreftelseMottatt(
    bekreftelse: Bekreftelse,
    periodeFunnet: Boolean,
    harAnsvar: Boolean
) {
    val dagerSidenForfall = Duration.between(Instant.now(), bekreftelse.svar.gjelderTil).toDays()
    val tags = Tags.of(
        Tag.of(LOESNING, bekreftelse.bekreftelsesloesning.name),
        Tag.of(FORTSETTE, bekreftelse.svar.vilFortsetteSomArbeidssoeker.toString()),
        Tag.of(HAR_JOBBET, bekreftelse.svar.harJobbetIDennePerioden.toString()),
        Tag.of(Labels.dager_siden_forfall, dagerSidenForfall.dagerSidenForfallString()),
        Tag.of(PERIODE_FUNNET, periodeFunnet.toString()),
        Tag.of(HAR_ANSVAR, harAnsvar.toString())
    )
    counter(bekreftelseMottattCounter, tags).increment()
}