package no.nav.paw.arbeidssokerregisteret.app.funksjoner

import no.nav.paw.arbeidssokerregisteret.app.config.ApplicationLogicConfig
import no.nav.paw.arbeidssokerregisteret.app.tilstand.InternTilstandOgApiTilstander
import no.nav.paw.arbeidssokerregisteret.app.tilstand.InternTilstandOgHendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.*

fun genererNyInternTilstandOgNyeApiTilstander(
    applicationLogicConfig: ApplicationLogicConfig,
    internTilstandOgHendelse: InternTilstandOgHendelse
): InternTilstandOgApiTilstander {
    val (scope, tilstand, hendelse) = internTilstandOgHendelse
    with(FunctionContext(tilstand, scope)) {
        return when (hendelse) {
            is Startet -> startPeriode(applicationLogicConfig.inkluderOpplysningerInnenforTidsvindu, hendelse)
            is Avsluttet -> avsluttPeriode(hendelse)
            is OpplysningerOmArbeidssoekerMottatt -> opplysningerOmArbeidssoekerMottatt(hendelse)
            is Avvist -> avvist(hendelse)
            is AvvistStoppAvPeriode -> ingenEndringEllerUtgaaendeMeldinger()
        }
    }
}

