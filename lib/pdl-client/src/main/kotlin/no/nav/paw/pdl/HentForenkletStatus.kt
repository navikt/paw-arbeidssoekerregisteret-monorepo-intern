package no.nav.paw.pdl

import no.nav.paw.pdl.graphql.generated.HentForenkletStatus
import no.nav.paw.pdl.graphql.generated.hentforenkletstatus.Person

suspend fun PdlClient.hentForenkletStatus(
    ident: String,
    callId: String?,
    traceparent: String? = null,
    navConsumerId: String?,
    behandlingsnummer: String,
): Person? {
    val query =
        HentForenkletStatus(
            HentForenkletStatus.Variables(ident),
        )

    val response =
        execute(
            query = query,
            callId = callId,
            navConsumerId = navConsumerId,
            traceparent = traceparent,
            behandlingsnummer = behandlingsnummer,
        )

    response.errors?.let {
        throw PdlException("'hentForenkletStatus' feilet", it)
    }

    logger.trace("Hentet 'hentForenkletStatus' fra PDL")

    return response
        .data
        ?.hentPerson
}
