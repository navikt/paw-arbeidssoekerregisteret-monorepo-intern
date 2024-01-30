package no.nav.paw.arbeidssokerregisteret.evaluering.regler.tilgangskontroll

import no.nav.paw.arbeidssokerregisteret.evaluering.Fakta
import no.nav.paw.arbeidssokerregisteret.evaluering.regler.Regel
import no.nav.paw.arbeidssokerregisteret.evaluering.regler.invoke

val tilgang: List<Regel> = listOf(
    "Ansatt har tilgang til bruker"(
        Fakta.ANSATT_TILGANG
    ),
    "Bruker prøver å endre for seg selv"(
        Fakta.SAMME_SOM_INNLOGGET_BRUKER,
        Fakta.IKKE_ANSATT
    )
)
