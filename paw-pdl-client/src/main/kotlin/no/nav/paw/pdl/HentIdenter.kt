package no.nav.paw.pdl

import no.nav.paw.pdl.graphql.generated.HentIdenter
import no.nav.paw.pdl.graphql.generated.enums.IdentGruppe
import no.nav.paw.pdl.graphql.generated.hentidenter.IdentInformasjon

suspend fun PdlClient.hentAktorId(
    ident: String,
    callId: String?,
    navConsumerId: String?,
): String? =
    hentIdenter(ident, callId, navConsumerId)
        ?.firstOrNull { it.gruppe == IdentGruppe.AKTORID }
        ?.ident

suspend fun PdlClient.hentIdenter(
    ident: String,
    callId: String?,
    navConsumerId: String?,
): List<IdentInformasjon>? {
    val query = HentIdenter(HentIdenter.Variables(ident))

    logger.info("Henter 'hentIdenter' fra PDL")

    val respons =
        execute(
            query = query,
            callId = callId,
            navConsumerId = navConsumerId,
            behandlingsnummer = "B123",
        )

    respons.errors?.let {
        logger.error("Henter 'hentIdenter' fra PDL feilet med: ${respons.errors}")
        throw PdlException("'hentIdenter' fra pdl feilet", it)
    }

    logger.info("Hentet 'hentIdenter' fra PDL")

    return respons
        .data
        ?.hentIdenter
        ?.identer
}
