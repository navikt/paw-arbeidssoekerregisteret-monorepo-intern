package no.nav.paw.arbeidssokerregisteret.app.tilstand

import no.nav.paw.arbeidssokerregisteret.api.v3.OpplysningerOmArbeidssoeker as ApiOpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode as ApiPeriode
import no.nav.paw.arbeidssokerregisteret.app.StreamHendelse
import no.nav.paw.arbeidssokerregisteret.app.funksjoner.HasRecordScope
import no.nav.paw.arbeidssokerregisteret.app.funksjoner.RecordScope

data class InternTilstandOgHendelse(
    override val recordScope: RecordScope<Long>,
    val tilstand: Tilstand?,
    val hendelse: StreamHendelse
): HasRecordScope<Long>

data class InternTilstandOgApiTilstander(
    val recordScope: RecordScope<Long>,
    val tilstand: Tilstand?,
    val nyPeriodeTilstand: ApiPeriode?,
    val nyOpplysningerOmArbeidssoekerTilstand: ApiOpplysningerOmArbeidssoeker?
)

