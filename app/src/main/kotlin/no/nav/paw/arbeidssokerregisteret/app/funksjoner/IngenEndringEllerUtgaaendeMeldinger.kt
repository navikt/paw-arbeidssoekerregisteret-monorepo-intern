package no.nav.paw.arbeidssokerregisteret.app.funksjoner

import no.nav.paw.arbeidssokerregisteret.app.tilstand.InternTilstandOgApiTilstander
import no.nav.paw.arbeidssokerregisteret.app.tilstand.Tilstand

context(RecordScope<Long>)
fun Tilstand?.ingenEndringEllerUtgaaendeMeldinger(): InternTilstandOgApiTilstander =
    InternTilstandOgApiTilstander(
        tilstand = this,
        nyPeriodeTilstand = null,
        nyOpplysningerOmArbeidssoekerTilstand = null,
        recordScope = currentScope()
    )