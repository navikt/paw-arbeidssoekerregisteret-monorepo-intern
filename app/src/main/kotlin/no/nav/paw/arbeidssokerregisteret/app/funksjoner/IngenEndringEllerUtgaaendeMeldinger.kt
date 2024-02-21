package no.nav.paw.arbeidssokerregisteret.app.funksjoner

import no.nav.paw.arbeidssokerregisteret.app.tilstand.InternTilstandOgApiTilstander
import no.nav.paw.arbeidssokerregisteret.app.tilstand.TilstandV1

context(RecordScope<Long>)
fun TilstandV1?.ingenEndringEllerUtgaaendeMeldinger(): InternTilstandOgApiTilstander =
    InternTilstandOgApiTilstander(
        tilstand = this,
        nyPeriodeTilstand = null,
        nyOpplysningerOmArbeidssoekerTilstand = null
    )