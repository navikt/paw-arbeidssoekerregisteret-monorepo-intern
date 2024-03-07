package no.nav.paw.arbeidssokerregisteret.app.funksjoner

import no.nav.paw.arbeidssokerregisteret.app.tilstand.InternTilstandOgApiTilstander
import no.nav.paw.arbeidssokerregisteret.app.tilstand.TilstandV1

context(HendelseScope<Long>)
fun TilstandV1?.ingenEndringEllerUtgaaendeMeldinger(): InternTilstandOgApiTilstander =
    InternTilstandOgApiTilstander(
        id = id,
        tilstand = this,
        nyPeriodeTilstand = null,
        nyOpplysningerOmArbeidssoekerTilstand = null
    )