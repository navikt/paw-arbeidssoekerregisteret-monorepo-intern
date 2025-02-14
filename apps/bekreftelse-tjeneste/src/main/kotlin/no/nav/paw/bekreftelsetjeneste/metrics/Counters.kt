package no.nav.paw.bekreftelsetjeneste.metrics

import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import no.nav.paw.bekreftelse.paavegneav.v1.PaaVegneAv
import no.nav.paw.bekreftelse.paavegneav.v1.vo.Start
import no.nav.paw.bekreftelse.paavegneav.v1.vo.Stopp
import no.nav.paw.bekreftelsetjeneste.metrics.Labels.Companion.ansvarlig
import no.nav.paw.bekreftelsetjeneste.paavegneav.InternPaaVegneAv
import no.nav.paw.bekreftelsetjeneste.paavegneav.Loesning
import java.time.Duration
import java.time.Instant


private const val bekreftelseMottattCounter = "arbeidssoekerregisteret_bekreftelse_mottatt_v1"
private const val paaVegneAvMottattCounter = "arbeidssoekerregisteret_paa_vegne_av_mottatt_v1"
private const val LOESNING = "loesning"
private const val FORTSETTE = "vil_fortsette"
private const val HAR_JOBBET = "har_jobbet"
private const val PERIODE_FUNNET = "periode_funnet"
private const val HAR_ANSVAR = "har_ansvar"
private const val HANDLING = "handling"


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

fun PrometheusMeterRegistry.tellPaVegneAv(
    paaVegneAv: PaaVegneAv,
    periodeFunnet: Boolean,
    ansvarlige: List<Loesning>
) {
    val handling = when (val handling = paaVegneAv.handling) {
        is Start -> "start"
        is Stopp -> "stopp"
        else -> handling::class.simpleName ?: "ukjent"
    }
    val (antallAnsvarlige, ansvarlig) = ansvar(ansvarlige)
    val tags = Tags.of(
        Tag.of(LOESNING, paaVegneAv.bekreftelsesloesning.name),
        Tag.of(HANDLING, handling),
        Tag.of(PERIODE_FUNNET, periodeFunnet.toString()),
        Tag.of(Labels.ansvarlig, ansvarlig),
        Tag.of(Labels.antall_ansvarlige, antallAnsvarlige.toString()),
        Tag.of(HAR_ANSVAR, (Loesning.from(paaVegneAv.bekreftelsesloesning) in ansvarlige).toString())
    )
    counter(paaVegneAvMottattCounter, tags).increment()
}