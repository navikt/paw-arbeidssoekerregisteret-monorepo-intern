package no.nav.paw.tilgangskontroll.poaotilgang.api

import java.util.UUID

data class NavAnsattTilgangTilEksternBrukerPolicyInputV2Dto(
    val navAnsattAzureId: UUID,
    val tilgangType: TilgangType,
    val norskIdent: String
): PolicyInput
