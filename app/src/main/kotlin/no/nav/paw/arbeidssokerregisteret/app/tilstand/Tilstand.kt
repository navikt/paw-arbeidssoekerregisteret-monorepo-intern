package no.nav.paw.arbeidssokerregisteret.app.tilstand

import no.nav.paw.arbeidssokerregisteret.app.tilInternTilstand
import no.nav.paw.arbeidssokerregisteret.app.tilstand.vo.Periode
import no.nav.paw.arbeidssokerregisteret.app.tilstand.vo.Situasjon
import no.nav.paw.arbeidssokerregisteret.intern.v1.Stoppet

data class Tilstand(
    val kafkaKey: Long,
    val gjeldeneTilstand: GjeldeneTilstand,
    val gjeldeneIdentitetsnummer: String,
    val allIdentitetsnummer: List<String>,
    val gjeldenePeriode: Periode?,
    val forrigePeriode: Periode?,
    val sisteSituasjon: Situasjon?,
    val forrigeSituasjon: Situasjon?
)
enum class GjeldeneTilstand {
    AVVIST, STARTET, STOPPET
}

fun Tilstand.avsluttGjeldenePeriode(stoppet: Stoppet): Tilstand {
    return copy(
        gjeldeneTilstand = GjeldeneTilstand.STOPPET,
        gjeldenePeriode = gjeldenePeriode?.copy(avsluttet = stoppet.metadata.tilInternTilstand())
    )
}

