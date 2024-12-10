package no.nav.paw.tilgangskontroll

import no.nav.paw.tilgangskontroll.vo.Identitetsnummer
import no.nav.paw.tilgangskontroll.vo.EntraId
import no.nav.paw.tilgangskontroll.vo.Tilgang

interface TilgangsTjenesteForAnsatte {
    suspend fun harAnsattTilgangTilPerson(
        navIdent: EntraId,
        identitetsnummer: Identitetsnummer,
        tilgang: Tilgang
    ): Boolean
}
