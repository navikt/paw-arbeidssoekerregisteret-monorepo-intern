package no.nav.paw.arbeidssokerregisteret.app.funksjoner

import no.nav.paw.arbeidssokerregisteret.app.tilstand.InternTilstandOgApiTilstander
import no.nav.paw.arbeidssokerregisteret.app.tilstand.TilstandV1

fun FunctionContext<TilstandV1?, Long>.ingenEndringEllerUtgaaendeMeldinger(): InternTilstandOgApiTilstander =
    InternTilstandOgApiTilstander(
        id = scope.id,
        tilstand = tilstand,
        nyPeriodeTilstand = null,
        nyOpplysningerOmArbeidssoekerTilstand = null
    )