package no.nav.paw.pdl

import no.nav.paw.pdl.graphql.generated.HentIdenter
import no.nav.paw.pdl.graphql.generated.enums.IdentGruppe
import no.nav.paw.pdl.graphql.generated.hentidenter.IdentInformasjon

suspend fun PdlClient.hentAktorId(
    ident: String,
    callId: String?,
    navConsumerId: String?,
    behandlingsnummer: String,
): String? =
    hentIdenter(
        ident = ident,
        callId = callId,
        navConsumerId = navConsumerId,
        behandlingsnummer = behandlingsnummer,
    )
        ?.firstOrNull { it.gruppe == IdentGruppe.AKTORID }
        ?.ident

suspend fun PdlClient.hentIdenter(
    ident: String,
    historikk: Boolean = false,
    callId: String?,
    navConsumerId: String?,
    behandlingsnummer: String,
): List<IdentInformasjon>? {
    val query = HentIdenter(HentIdenter.Variables(ident = ident, historisk = historikk))

    logger.info("Henter 'hentIdenter' fra PDL")

    val respons =
        execute(
            query = query,
            callId = callId,
            navConsumerId = navConsumerId,
            behandlingsnummer = behandlingsnummer,
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
