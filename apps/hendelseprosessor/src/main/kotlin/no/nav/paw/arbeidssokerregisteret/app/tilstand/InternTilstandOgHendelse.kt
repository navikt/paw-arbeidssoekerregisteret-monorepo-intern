package no.nav.paw.arbeidssokerregisteret.app.tilstand

import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker as ApiOpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode as ApiPeriode
import no.nav.paw.arbeidssokerregisteret.app.StreamHendelse
import no.nav.paw.arbeidssokerregisteret.app.funksjoner.HasRecordScope
import no.nav.paw.arbeidssokerregisteret.app.funksjoner.HendelseScope

data class InternTilstandOgHendelse(
    override val hendelseScope: HendelseScope<Long>,
    val tilstand: TilstandV1?,
    val hendelse: StreamHendelse
): HasRecordScope<Long>

data class InternTilstandOgApiTilstander(
    val tilstand: TilstandV1?,
    /**Unik id for personen som tilstanden gjelder for, generert av paw-kafka-key-generator*/
    val id: Long,
    val nyPeriodeTilstand: ApiPeriode?,
    val nyOpplysningerOmArbeidssoekerTilstand: ApiOpplysningerOmArbeidssoeker?
)

