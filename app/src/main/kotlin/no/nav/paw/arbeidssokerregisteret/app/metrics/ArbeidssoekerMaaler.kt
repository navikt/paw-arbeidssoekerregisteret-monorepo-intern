package no.nav.paw.arbeidssokerregisteret.app.metrics

import io.micrometer.core.instrument.Tag
import no.nav.paw.arbeidssokerregisteret.app.tilstand.Tilstand

data class ArbeidssoekerMaaler(
    override val partition: Int?,
    val varighet: String,
    val antallSituasjoner: String
): WithMetricsInfo {
    override val name: String get() = Names.ARBEIDSSOEKER_ANTALL
    override val labels: List<Tag> get() = listOf(
        Tag.of("varighet", varighet),
        Tag.of("antallSituasjoner", antallSituasjoner))
}

fun arbeidssokerMaaler(tilstand: Tilstand): ArbeidssoekerMaaler? {
    if (tilstand.gjeldenePeriode == null) return null
    if (tilstand.gjeldenePeriode.avsluttet != null) return null
    val varighet = durationToBucket(tilstand.gjeldenePeriode.startet.tidspunkt)
    val antallSituasjoner = tilstand.sisteOpplysningerOmArbeidssoeker
        ?.jobbsituasjon?.beskrivelser?.size ?: 0
    val antall = if (antallSituasjoner <= 10) {
        antallSituasjoner.toString()
    } else {
        "10+"
    }
    return ArbeidssoekerMaaler(
        partition = tilstand.recordScope?.partition,
        varighet = varighet,
        antallSituasjoner = antall
    )
}

/**
 * # HELP paw_arbeidssokerregisteret_latency
 * # TYPE paw_arbeidssokerregisteret_latency gauge
 * paw_arbeidssokerregisteret_latency{partition="0",topic="opplysninger-om-arbeidssoeker",} 2293.0
 * paw_arbeidssokerregisteret_latency{partition="0",topic="periode",} 6.0
 * # HELP paw_arbeidssokerregisteret_message_total
 * # TYPE paw_arbeidssokerregisteret_message_total counter
 * paw_arbeidssokerregisteret_message_total{action="avslutt_periode",direction="out",topic="periode",type="no.nav.paw.arbeidssokerregisteret.api.v1.Periode",} 3.0
 * paw_arbeidssokerregisteret_message_total{action="start_periode",direction="in",topic="eventlog",type="intern.v1.startet",} 7.0
 * paw_arbeidssokerregisteret_message_total{action="start_periode",direction="out",topic="periode",type="no.nav.paw.arbeidssokerregisteret.api.v1.Periode",} 4.0
 * paw_arbeidssokerregisteret_message_total{action="opplysninger_mottatt",direction="out",topic="opplysninger-om-arbeidssoeker",type="no.nav.paw.arbeidssokerregisteret.api.v1.OpplysningerOmArbeidssoeker",} 1.0
 * paw_arbeidssokerregisteret_message_total{action="avslutt_periode",direction="in",topic="eventlog",type="intern.v1.avsluttet",} 3.0
 * paw_arbeidssokerregisteret_message_total{action="opplysninger_mottatt",direction="in",topic="eventlog",type="intern.v1.opplysninger_om_arbeidssoeker",} 1.0
 * # HELP paw_arbeidssokerregisteret_arbeidssoker_antall
 * # TYPE paw_arbeidssokerregisteret_arbeidssoker_antall gauge
 * paw_arbeidssokerregisteret_arbeidssoker_antall{antallSituasjoner="0",varighet="under_6_maaneder",} 1.0
 * paw_arbeidssokerregisteret_arbeidssoker_antall{antallSituasjoner="2",varighet="under_6_maaneder",} 3.0
 * # HELP paw_arbeidssokerregisteret_arbeidssoker_jobb_situasjon
 * # TYPE paw_arbeidssokerregisteret_arbeidssoker_jobb_situasjon gauge
 * paw_arbeidssokerregisteret_arbeidssoker_jobb_situasjon{jobbSituasjon="DELTIDSJOBB_VIL_MER",prosent="25-50_prosent",styrk08="NA",varighet="under_6_maaneder",} 3.0
 * paw_arbeidssokerregisteret_arbeidssoker_jobb_situasjon{jobbSituasjon="ER_PERMITTERT",prosent="100_prosent",styrk08="1",varighet="under_6_maaneder",} 3.0
 *
 */