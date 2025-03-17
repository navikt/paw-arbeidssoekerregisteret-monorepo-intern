package no.nav.paw.arbeidssokerregisteret.app.metrics

import io.micrometer.core.instrument.Tag
import no.nav.paw.arbeidssokerregisteret.PROSENT
import no.nav.paw.arbeidssokerregisteret.STILLING_STYRK08
import no.nav.paw.arbeidssokerregisteret.app.funksjoner.isWithinWindow
import no.nav.paw.arbeidssokerregisteret.app.tilstand.GjeldeneTilstand
import no.nav.paw.arbeidssokerregisteret.app.tilstand.TilstandV1
import java.time.Duration
import java.time.Instant

data class ManglerOpplysningerMaaler(
    override val partition: Int?,
    val manglerOpplysninger: Boolean
): WithMetricsInfo {
    override val name: String get() = Names.MANGLER_OPPLYSNINGER
    override val labels: List<Tag> get() = listOf(
        Tag.of("mangler_opplysninger", manglerOpplysninger.toString()),
    )
}

fun manglerOpplysningerMaaler(opplysningTidsvindu: Duration, tilstand: TilstandV1): List<ManglerOpplysningerMaaler> {
    return listOfNotNull(
        tilstand
            .takeIf { it.gjeldeneTilstand == GjeldeneTilstand.STARTET}
            ?.let { aktiv ->
                val startet = aktiv.gjeldenePeriode?.startet?.tidspunkt
                val sisteOpplysning = aktiv.sisteOpplysningerOmArbeidssoeker?.metadata?.tidspunkt
                when {
                    startet == null -> null
                    sisteOpplysning == null -> ManglerOpplysningerMaaler(partition = tilstand.hendelseScope.partition, manglerOpplysninger = true)
                    else -> {
                        val mangler =
                            sisteOpplysning.isAfter(startet) ||
                            !(opplysningTidsvindu.isWithinWindow(sisteOpplysning, startet))
                        ManglerOpplysningerMaaler(partition = tilstand.hendelseScope.partition, manglerOpplysninger = mangler)
                    }
                }
            }
    )
}
