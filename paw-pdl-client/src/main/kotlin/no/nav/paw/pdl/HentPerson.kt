package no.nav.paw.pdl

import no.nav.paw.pdl.graphql.generated.HentPerson
import no.nav.paw.pdl.graphql.generated.hentperson.Person

suspend fun PdlClient.hentPerson(ident: String, callId: String?, navConsumerId: String?): Person? {
    val query = HentPerson(HentPerson.Variables(ident))

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
}
