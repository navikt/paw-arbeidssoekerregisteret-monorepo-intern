package no.nav.paw.pdl

import no.nav.paw.pdl.graphql.generated.HentForenkletStatusBolk
import no.nav.paw.pdl.graphql.generated.hentforenkletstatusbolk.HentPersonBolkResult

suspend fun PdlClient.hentForenkletStatusBolk(
    ident: List<String>,
    callId: String?,
    traceparent: String? = null,
    navConsumerId: String?,
    behandlingsnummer: String,
): List<HentPersonBolkResult>? {
    val query =
        HentForenkletStatusBolk(
            HentForenkletStatusBolk.Variables(ident),
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
        throw PdlException("'hentForenkletStatusBolk' feilet", it)
    }

    logger.trace("Hentet 'hentForenkletStatusBolk' fra PDL")

    return response
        .data
        ?.hentPersonBolk
}
