package no.nav.paw.arbeidssokerregisteret.app.tilstand

import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Situasjon

data class Tilstand(
    val kafkaKey: Long,
    val gjeldeneTilstand: GjeldeneTilstand,
    val gjeldeneIdentitetsnummer: String,
    val allIdentitetsnummer: Set<String>,
    val gjeldenePeriode: Periode?,
    val forrigePeriode: Periode?,
    val sisteSituasjon: Situasjon?,
    val forrigeSituasjon: Situasjon?
)
enum class GjeldeneTilstand {
    AVVIST, STARTET, STOPPET
}

