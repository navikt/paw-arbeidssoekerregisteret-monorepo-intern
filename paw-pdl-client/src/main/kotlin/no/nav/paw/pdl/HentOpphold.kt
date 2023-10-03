package no.nav.paw.pdl

import no.nav.paw.pdl.graphql.generated.HentOpphold
import no.nav.paw.pdl.graphql.generated.hentopphold.Opphold

suspend fun PdlClient.hentOpphold(ident: String, callId: String?, navConsumerId: String?): List<Opphold>? {
    val query = HentOpphold(HentOpphold.Variables(ident))

    logger.info("Henter 'hentPerson' fra PDL")

    val respons = execute(query, callId, navConsumerId)

    respons.errors?.let {
        logger.error("Henter 'hentPerson' fra PDL feilet med: ${respons.errors}")
        throw PdlException(it)
    }

    logger.info("Hentet 'hentPerson' fra PDL")

    return respons
        .data
        ?.hentPerson
        ?.opphold
}
