package no.nav.paw.arbeidssokerregisteret.app.tilstand

import no.nav.paw.arbeidssokerregisteret.app.tilstand.vo.Periode
import no.nav.paw.arbeidssokerregisteret.app.tilstand.vo.Situasjon

data class Tilstand(
    val kafkaKey: Long,
    val gjeldeneIdentitetsnummer: String,
    val allIdentitetsnummer: List<String>,
    val gjeldendePeriode: Periode?,
    val forrigePeriode: Periode?,
    val gjeldeneSituasjon: Situasjon?,
    val forrigeSituasjon: Situasjon?
)

