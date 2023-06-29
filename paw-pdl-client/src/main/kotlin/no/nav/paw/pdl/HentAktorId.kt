package no.nav.paw.pdl

import no.nav.paw.pdl.graphql.generated.HentIdenter
import no.nav.paw.pdl.graphql.generated.enums.IdentGruppe
import no.nav.paw.pdl.graphql.generated.hentidenter.IdentInformasjon

suspend fun PdlClient.hentAktorId(ident: String, callId: String): String? = hentIdenter(ident, callId)
    ?.firstOrNull { it.gruppe == IdentGruppe.AKTORID }
    ?.ident

suspend fun PdlClient.hentIdenter(ident: String, callId: String): List<IdentInformasjon>? {
    val query = HentIdenter(HentIdenter.Variables(ident))

    logger.info("Henter 'hentIdenter' fra PDL")

    val respons = execute(query, callId)

    respons.errors?.let {
        logger.error("Henter 'hentIdenter' fra PDL feilet med: ${respons.errors}")
        throw PdlException(it)
    }

    logger.info("Hentet 'hentIdenter' fra PDL")

    return respons
        .data
        ?.hentIdenter
        ?.identer
}
