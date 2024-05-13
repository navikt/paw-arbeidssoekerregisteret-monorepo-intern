package no.nav.paw.pdl

import no.nav.paw.pdl.graphql.generated.HentPerson
import no.nav.paw.pdl.graphql.generated.hentperson.Person

suspend fun PdlClient.hentPerson(
    ident: String,
    callId: String?,
    traceparent: String? = null,
    navConsumerId: String?,
    historisk: Boolean = false,
    behandlingsnummer: String,
): Person? {
    val query =
        HentPerson(
            HentPerson.Variables(ident, historisk),
        )

    logger.trace("Henter 'hentPerson' fra PDL")

    val respons =
        execute(
            query = query,
            callId = callId,
            navConsumerId = navConsumerId,
            traceparent = traceparent,
            behandlingsnummer = behandlingsnummer,
        )

    respons.errors?.let {
        throw PdlException("'hentPerson' feilet", it)
    }

    logger.trace("Hentet 'hentPerson' fra PDL")

    return respons
        .data
        ?.hentPerson
}
