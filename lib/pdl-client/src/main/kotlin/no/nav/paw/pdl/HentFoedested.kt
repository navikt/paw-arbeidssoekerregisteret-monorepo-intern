package no.nav.paw.pdl

import no.nav.paw.pdl.graphql.generated.HentFoedested
import no.nav.paw.pdl.graphql.generated.hentfoedested.Foedested

suspend fun PdlClient.hentFoedested(
    ident: String,
    callId: String?,
    traceparent: String? = null,
    navConsumerId: String?,
    behandlingsnummer: String,
): Foedested? {
    val query =
        HentFoedested(
            HentFoedested.Variables(ident),
        )

    logger.trace("Henter 'hentFoedested' fra PDL")

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

    logger.trace("Hentet 'hentFoedested' fra PDL")

    return respons
        .data
        ?.hentPerson
        ?.foedested
        ?.firstOrNull()
}