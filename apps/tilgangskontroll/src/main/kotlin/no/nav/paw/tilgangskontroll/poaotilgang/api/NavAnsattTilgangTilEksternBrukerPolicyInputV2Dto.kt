package no.nav.paw.tilgangskontroll.poaotilgang.api

import no.nav.paw.felles.model.Identitetsnummer
import no.nav.paw.felles.model.NavIdent
import no.nav.paw.tilgangskontroll.vo.Tilgang
import java.util.*

data class NavAnsattNavIdentTilgangTilEksternBrukerPolicyInputV1Dto(
    val navIdent: String,
    val norskIdent: String
) : PolicyInput

fun navAnsattTilgangTilEksternBrukerPolicyInputV1Dto(
    navIdent: NavIdent,
    identitetsnummer: Identitetsnummer,
    tilgang: Tilgang
): List<PolicyEvaluationRequestDto<PolicyInput>> {
    return tilgang
        .tilEksternPolicyId
        .map { policyId ->
            PolicyEvaluationRequestDto(
                requestId = UUID.randomUUID(),
                policyInput = NavAnsattNavIdentTilgangTilEksternBrukerPolicyInputV1Dto(
                    navIdent = navIdent.value,
                    norskIdent = identitetsnummer.value
                ),
                policyId = policyId
            )
        }
}

val Tilgang.tilEksternPolicyId: Set<PolicyId>
    get() = when (this) {
        Tilgang.LESE -> setOf(PolicyId.NAV_ANSATT_NAV_IDENT_LESETILGANG_TIL_EKSTERN_BRUKER_V1)
        Tilgang.SKRIVE -> setOf(PolicyId.NAV_ANSATT_NAV_IDENT_SKRIVETILGANG_TIL_EKSTERN_BRUKER_V1)
        Tilgang.LESE_SKRIVE -> setOf(
            PolicyId.NAV_ANSATT_NAV_IDENT_LESETILGANG_TIL_EKSTERN_BRUKER_V1,
            PolicyId.NAV_ANSATT_NAV_IDENT_SKRIVETILGANG_TIL_EKSTERN_BRUKER_V1
        )
    }
