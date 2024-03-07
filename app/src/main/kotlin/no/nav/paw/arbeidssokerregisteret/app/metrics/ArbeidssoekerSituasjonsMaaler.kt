package no.nav.paw.arbeidssokerregisteret.app.metrics

import io.micrometer.core.instrument.Tag
import no.nav.paw.arbeidssokerregisteret.PROSENT
import no.nav.paw.arbeidssokerregisteret.STILLING_STYRK08
import no.nav.paw.arbeidssokerregisteret.app.tilstand.TilstandV1

data class ArbeidssoekerSituasjonsMaaler(
    override val partition: Int?,
    val varighet: String,
    val jobbSituasjon: String,
    val prosent: String,
    val styrk08: String
): WithMetricsInfo {
    override val name: String get() = Names.ARBEIDSSOEKER_JOBB_SITUASJON
    override val labels: List<Tag> get() = listOf(
        Tag.of("varighet", varighet),
        Tag.of("jobbSituasjon", jobbSituasjon),
        Tag.of("prosent", prosent),
        Tag.of("styrk08", styrk08)
    )
}

fun arbeidssoekerSituasjonsMaaler(tilstand: TilstandV1): List<ArbeidssoekerSituasjonsMaaler> {
    if (tilstand.gjeldenePeriode == null) return emptyList()
    if (tilstand.gjeldenePeriode.avsluttet != null) return emptyList()
    val varighet = durationToBucket(tilstand.gjeldenePeriode.startet.tidspunkt)
    return tilstand.sisteOpplysningerOmArbeidssoeker
        ?.jobbsituasjon
        ?.beskrivelser
        ?.map { jobbsituasjonMedDetaljer ->
            ArbeidssoekerSituasjonsMaaler(
                partition = tilstand.hendelseScope.partition,
                varighet = varighet,
                jobbSituasjon = jobbsituasjonMedDetaljer.beskrivelse.name,
                prosent = percentageToBucket(jobbsituasjonMedDetaljer.detaljer[PROSENT]),
                styrk08 = jobbsituasjonMedDetaljer.detaljer[STILLING_STYRK08]
                    ?.firstOrNull()?.toString() ?: NOT_APPLICABLE
            )
        } ?: emptyList()
}
