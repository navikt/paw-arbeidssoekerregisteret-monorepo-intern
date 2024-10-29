package no.nav.paw.bekreftelsetjeneste.paavegneav

import no.nav.paw.bekreftelse.paavegneav.v1.vo.Bekreftelsesloesning
import java.time.Duration
import java.util.*

data class PaaVegneAvTilstand(
    val periodeId: UUID,
    val paaVegneAvList: List<InternPaaVegneAv>
)

data class InternPaaVegneAv(
    val loesning: Loesning,
    val intervall: Duration,
    val gracePeriode: Duration
)

enum class Loesning {
    UKJENT_VERDI,
    ARBEIDSSOEKERREGISTERET,
    DAGPENGER;

    companion object {
        fun from(value: Bekreftelsesloesning): Loesning = when (value) {
            Bekreftelsesloesning.UKJENT_VERDI -> UKJENT_VERDI
            Bekreftelsesloesning.ARBEIDSSOEKERREGISTERET -> ARBEIDSSOEKERREGISTERET
            Bekreftelsesloesning.DAGPENGER -> DAGPENGER
        }
        fun from(value: no.nav.paw.bekreftelse.melding.v1.vo.Bekreftelsesloesning): Loesning = when (value) {
            no.nav.paw.bekreftelse.melding.v1.vo.Bekreftelsesloesning.UKJENT_VERDI -> UKJENT_VERDI
            no.nav.paw.bekreftelse.melding.v1.vo.Bekreftelsesloesning.ARBEIDSSOEKERREGISTERET -> ARBEIDSSOEKERREGISTERET
            no.nav.paw.bekreftelse.melding.v1.vo.Bekreftelsesloesning.DAGPENGER -> DAGPENGER
        }
    }
}

fun opprettPaaVegneAvTilstand(
    periodeId: UUID,
    paaVegneAv: InternPaaVegneAv? = null
): PaaVegneAvTilstand = PaaVegneAvTilstand(
    periodeId = periodeId,
    paaVegneAvList = listOfNotNull(paaVegneAv)
)

operator fun PaaVegneAvTilstand.plus(paaVegneAv: InternPaaVegneAv): PaaVegneAvTilstand =
    copy(paaVegneAvList = paaVegneAvList
        .filterNot { it.loesning == paaVegneAv.loesning} + paaVegneAv
    )

operator fun PaaVegneAvTilstand?.minus(loesning: Loesning): PaaVegneAvTilstand? =
    this?.paaVegneAvList
        ?.filterNot { it.loesning == loesning }
        ?.takeIf(List<InternPaaVegneAv>::isNotEmpty)
        ?.let { copy(paaVegneAvList = it) }
