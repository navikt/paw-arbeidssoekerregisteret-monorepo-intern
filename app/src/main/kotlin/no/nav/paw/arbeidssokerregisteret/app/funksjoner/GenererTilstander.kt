package no.nav.paw.arbeidssokerregisteret.app.funksjoner

import no.nav.paw.arbeidssokerregisteret.app.config.ApplicationLogicConfig
import no.nav.paw.arbeidssokerregisteret.app.tilstand.InternTilstandOgApiTilstander
import no.nav.paw.arbeidssokerregisteret.app.tilstand.InternTilstandOgHendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.Avsluttet
import no.nav.paw.arbeidssokerregisteret.intern.v1.Avvist
import no.nav.paw.arbeidssokerregisteret.intern.v1.OpplysningerOmArbeidssoekerMottatt
import no.nav.paw.arbeidssokerregisteret.intern.v1.Startet

fun genererNyInternTilstandOgNyeApiTilstander(
    applicationLogicConfig: ApplicationLogicConfig,
    internTilstandOgHendelse: InternTilstandOgHendelse
): InternTilstandOgApiTilstander {
    val (scope, tilstand, hendelse) = internTilstandOgHendelse
    with(scope) {
        return when (hendelse) {
            is Startet -> tilstand.startPeriode(applicationLogicConfig.inkluderOpplysningerInnenforTidsvindu, hendelse)
            is Avsluttet -> tilstand.avsluttPeriode(hendelse)
            is OpplysningerOmArbeidssoekerMottatt -> tilstand.opplysningerOmArbeidssoekerMottatt(hendelse)
            is Avvist -> tilstand.avvist(hendelse)
            else -> throw IllegalStateException("Uventet hendelse: $hendelse")
        }
    }
}

