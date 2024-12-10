package no.nav.paw.tilgangskontroll.poaotilgang.api

import no.nav.paw.tilgangskontroll.vo.Identitetsnummer
import no.nav.paw.tilgangskontroll.vo.EntraId
import no.nav.paw.tilgangskontroll.vo.Tilgang
import java.util.UUID

data class NavAnsattTilgangTilEksternBrukerPolicyInputV2Dto(
    val navAnsattAzureId: UUID,
    val tilgangType: TilgangType,
    val norskIdent: String
): PolicyInput

fun navAnsattTilgangTilEksternBrukerPolicyInputV2Dto(
    navIdent: EntraId,
    identitetsnummer: Identitetsnummer,
    tilgang: Tilgang
): List<PolicyEvaluationRequestDto<NavAnsattTilgangTilEksternBrukerPolicyInputV2Dto>> {
    return tilgang.tilEksternTilgangType
        .map { tilgangType ->
            PolicyEvaluationRequestDto(
                requestId = UUID.randomUUID(),
                policyInput = NavAnsattTilgangTilEksternBrukerPolicyInputV2Dto(
                    navAnsattAzureId = navIdent.value,
                    norskIdent = identitetsnummer.value,
                    tilgangType = tilgangType
                ),
                policyId = PolicyId.NAV_ANSATT_TILGANG_TIL_EKSTERN_BRUKER_V2
            )
        }
}

val Tilgang.tilEksternTilgangType: Set<TilgangType>
    get() = when (this) {
        Tilgang.LESE -> setOf(TilgangType.LESE)
        Tilgang.SKRIVE -> setOf(TilgangType.SKRIVE)
        Tilgang.LESE_SKRIVE -> setOf(TilgangType.LESE, TilgangType.SKRIVE)
    }
