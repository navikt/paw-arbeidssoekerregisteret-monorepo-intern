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

    val resultat = execute(query)

    if (!resultat.errors.isNullOrEmpty()) {
        logger.error("Henter 'aktorId' fra PDL feilet med: ${resultat.errors}")
        throw PdlException(resultat.errors)
    }

    logger.info("Hentet 'aktorId' fra PDL")

    return resultat
        .data
        ?.hentIdenter
        ?.identer
}
