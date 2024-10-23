package no.nav.paw.bekreftelsetjeneste.ansvar

import no.nav.paw.bekreftelse.ansvar.v1.vo.Bekreftelsesloesning
import java.time.Duration
import java.util.*

data class Ansvar(
    val periodeId: UUID,
    val ansvarlige: List<Ansvarlig>
)

data class Ansvarlig(
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

fun ansvar(
    periodeId: UUID,
    ansvarlig: Ansvarlig? = null
): Ansvar = Ansvar(
    periodeId = periodeId,
    ansvarlige = listOfNotNull(ansvarlig)
)

operator fun Ansvar.plus(ansvarlig: Ansvarlig): Ansvar =
    copy(ansvarlige = ansvarlige
        .filterNot { it.loesning == ansvarlig.loesning} + ansvarlig
    )

operator fun Ansvar?.minus(loesning: Loesning): Ansvar? =
    this?.ansvarlige
        ?.filterNot { it.loesning == loesning }
        ?.takeIf(List<Ansvarlig>::isNotEmpty)
        ?.let { copy(ansvarlige = it) }
