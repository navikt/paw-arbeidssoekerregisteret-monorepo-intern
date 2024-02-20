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
        partition = tilstand.recordScope.partition,
        varighet = varighet,
        antallSituasjoner = antall
    )
}
