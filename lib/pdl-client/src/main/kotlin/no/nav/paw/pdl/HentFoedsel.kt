package no.nav.paw.pdl

import no.nav.paw.pdl.graphql.generated.HentFoedsel
import no.nav.paw.pdl.graphql.generated.hentfoedsel.Foedsel

suspend fun PdlClient.hentFoedsel(
    ident: String,
    callId: String?,
    traceparent: String? = null,
    navConsumerId: String?,
    behandlingsnummer: String,
): Foedsel? {
    val query =
        HentFoedsel(
            HentFoedsel.Variables(ident),
        )

    logger.trace("Henter 'hentFoedsel' fra PDL")

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

    logger.trace("Hentet 'hentFoedsel' fra PDL")

    return respons
        .data
        ?.hentPerson
        ?.foedsel
        ?.firstOrNull()
}
