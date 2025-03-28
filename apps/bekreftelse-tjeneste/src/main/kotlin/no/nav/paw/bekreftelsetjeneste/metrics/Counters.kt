package no.nav.paw.bekreftelsetjeneste.metrics

import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import no.nav.paw.bekreftelse.paavegneav.v1.PaaVegneAv
import no.nav.paw.bekreftelse.paavegneav.v1.vo.Start
import no.nav.paw.bekreftelse.paavegneav.v1.vo.Stopp
import no.nav.paw.bekreftelsetjeneste.paavegneav.Loesning
import java.time.Duration
import java.time.Instant

private const val bekreftelseUtgaaendeHendelseCounter = "arbeidssoekerregisteret_bekreftelse_utgaaende_hendelse_v1"

private const val bekreftelseMottattCounter = "arbeidssoekerregisteret_bekreftelse_mottatt_v1"
private const val paaVegneAvMottattCounter = "arbeidssoekerregisteret_paa_vegne_av_mottatt_v1"
private const val LOESNING = "loesning"
private const val FORTSETTE = "vil_fortsette"
private const val HAR_JOBBET = "har_jobbet"
private const val PERIODE_FUNNET = "periode_funnet"
private const val HAR_ANSVAR = "har_ansvar"
private const val HANDLING = "handling"
private const val FRIST_BRUTT = "frist_brutt"

private const val GENRISK_BERKREFTELSE_HANDLING = "arbeidssoekerregisteret_bekreftelse_handling"

fun PrometheusMeterRegistry.tellBekreftelseHandling(
    handling: String
) {
    val tags = Tags.of(
        Tag.of(HANDLING, handling)
    )
    counter(GENRISK_BERKREFTELSE_HANDLING, tags).increment()
}

fun PrometheusMeterRegistry.tellBekreftelseUtgaaendeHendelse(
    bekreftelseHendelse: BekreftelseHendelse
) {
    val tags = Tags.of(
        Tag.of("hendelse_type", bekreftelseHendelse.hendelseType)
    )
    counter(bekreftelseUtgaaendeHendelseCounter, tags).increment()
}

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
        Tag.of(HAR_ANSVAR, (Loesning.from(paaVegneAv.bekreftelsesloesning) in ansvarlige).toString()),
        Tag.of(FRIST_BRUTT, (paaVegneAv.handling as? Stopp)?.fristBrutt?.toString() ?: "NA")
    )
    counter(paaVegneAvMottattCounter, tags).increment()
}