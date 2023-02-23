package no.nav.paw.pdl

import no.nav.paw.pdl.graphql.generated.HentIdenter
import no.nav.paw.pdl.graphql.generated.enums.IdentGruppe
import no.nav.paw.pdl.graphql.generated.hentidenter.IdentInformasjon

suspend fun PdlClient.hentAktorId(ident: String): String? = hentIdenter(ident)
    ?.firstOrNull { it.gruppe == IdentGruppe.AKTORID }
    ?.ident

suspend fun PdlClient.hentIdenter(ident: String): List<IdentInformasjon>? {
    val query = HentIdenter(HentIdenter.Variables(ident))

    logger.info("Henter 'aktorId' fra PDL")

    val respons = execute(query)

    respons.errors?.let {
        logger.error("Henter 'aktorId' fra PDL feilet med: ${respons.errors}")
        throw PdlException(it)
    }

    logger.info("Hentet 'aktorId' fra PDL")

    return respons
        .data
        ?.hentIdenter
        ?.identer
}
