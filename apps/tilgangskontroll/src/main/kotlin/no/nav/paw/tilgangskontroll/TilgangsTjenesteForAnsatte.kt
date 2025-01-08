package no.nav.paw.tilgangskontroll

import no.nav.paw.tilgangskontroll.vo.Identitetsnummer
import no.nav.paw.tilgangskontroll.vo.NavIdent
import no.nav.paw.tilgangskontroll.vo.Tilgang

interface TilgangsTjenesteForAnsatte {
    suspend fun harAnsattTilgangTilPerson(
        navIdent: NavIdent,
        identitetsnummer: Identitetsnummer,
        tilgang: Tilgang
    ): Boolean
}
