package no.nav.paw.arbeidssokerregisteret.app.tilstand

import no.nav.paw.arbeidssokerregisteret.app.funksjoner.RecordScope
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.OpplysningerOmArbeidssoeker

data class Tilstand(
    val recordScope: RecordScope<Long>?,
    val gjeldeneTilstand: GjeldeneTilstand,
    val gjeldeneIdentitetsnummer: String,
    val allIdentitetsnummer: Set<String>,
    val gjeldenePeriode: Periode?,
    val forrigePeriode: Periode?,
    val sisteOpplysningerOmArbeidssoeker: OpplysningerOmArbeidssoeker?,
    val forrigeOpplysningerOmArbeidssoeker: OpplysningerOmArbeidssoeker?
)
enum class GjeldeneTilstand {
    AVVIST, STARTET, STOPPET
}

