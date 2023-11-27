package no.nav.paw.arbeidssokerregisteret.app.tilstand

import no.nav.paw.arbeidssokerregisteret.api.v1.OpplysningerOmArbeidssoeker as ApiOpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode as ApiPeriode
import no.nav.paw.arbeidssokerregisteret.app.StreamHendelse

data class InternTilstandOgHendelse(val tilstand: Tilstand?, val hendelse: StreamHendelse)

data class InternTilstandOgApiTilstander(
    val tilstand: Tilstand?,
    val nyePeriodeTilstand: ApiPeriode?,
    val nyOpplysningerOmArbeidssoekerTilstand: ApiOpplysningerOmArbeidssoeker?
)

