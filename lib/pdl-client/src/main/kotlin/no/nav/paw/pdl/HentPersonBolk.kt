package no.nav.paw.pdl

import no.nav.paw.pdl.graphql.generated.HentPersonBolk
import no.nav.paw.pdl.graphql.generated.hentpersonbolk.HentPersonBolkResult

suspend fun PdlClient.hentPersonBolk(
    ident: List<String>,
    callId: String?,
    traceparent: String? = null,
    navConsumerId: String?,
    historisk: Boolean = false,
    behandlingsnummer: String,
): List<HentPersonBolkResult>? {
    val query =
        HentPersonBolk(
            HentPersonBolk.Variables(ident, historisk),
        )

    logger.trace("Henter 'hentPersonBolk' fra PDL")

    val respons =
        execute(
            query = query,
            callId = callId,
            navConsumerId = navConsumerId,
            traceparent = traceparent,
            behandlingsnummer = behandlingsnummer,
        )

    respons.errors?.let {
        throw PdlException("'hentPersonBolk' fra PDL feilet", it)
    }

    logger.trace("Hentet 'hentPersonBolk' fra PDL")

    return respons
        .data
        ?.hentPersonBolk
}
