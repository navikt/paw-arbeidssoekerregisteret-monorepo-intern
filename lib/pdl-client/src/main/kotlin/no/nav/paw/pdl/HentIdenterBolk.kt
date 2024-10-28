package no.nav.paw.pdl

import no.nav.paw.pdl.graphql.generated.HentIdenterBolk
import no.nav.paw.pdl.graphql.generated.enums.IdentGruppe
import no.nav.paw.pdl.graphql.generated.hentidenter.IdentInformasjon

suspend fun PdlClient.hentIdenterBolk(
    identer: List<String>,
    grupper: List<IdentGruppe> = listOf(IdentGruppe.FOLKEREGISTERIDENT),
    historikk: Boolean = true,
    callId: String?,
    navConsumerId: String?,
    behandlingsnummer: String,
): Map<String, List<IdentInformasjon>> {
    val request = HentIdenterBolk(
        HentIdenterBolk.Variables(
            identer = identer,
            grupper = grupper,
            historisk = historikk,
        )
    )
    val response = execute(
        query = request,
        callId = callId,
        navConsumerId = navConsumerId,
        behandlingsnummer = behandlingsnummer,
    )

    response.errors?.let {
        logger.error("Henter 'hentIdenter' fra PDL feilet med: ${response.errors}")
        throw PdlException("'hentIdenter' fra pdl feilet", it)
    }

    logger.info("Hentet 'hentIdenter' fra PDL")

    return response
        .data
        ?.hentIdenterBolk
        ?.map { data ->
            data.ident to (data.identer ?: emptyList()).map { identInfoBolk ->
                IdentInformasjon(
                    ident = identInfoBolk.ident,
                    gruppe = identInfoBolk.gruppe,
                    historisk = identInfoBolk.historisk,
                )
            }
        }?.toMap() ?: emptyMap()
}
