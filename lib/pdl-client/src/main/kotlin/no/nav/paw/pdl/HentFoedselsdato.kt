package no.nav.paw.pdl

import no.nav.paw.pdl.graphql.generated.hentfoedselsdato.Foedselsdato
import no.nav.paw.pdl.graphql.generated.HentFoedselsdato

suspend fun PdlClient.hentFoedselsdato(
    ident: String,
    callId: String?,
    traceparent: String? = null,
    navConsumerId: String?,
    behandlingsnummer: String,
): Foedselsdato? {
    val query =
        HentFoedselsdato(
            HentFoedselsdato.Variables(ident),
        )

    logger.trace("Henter 'hentFoedselsdato' fra PDL")

    val respons =
        execute(
            query = query,
            callId = callId,
            navConsumerId = navConsumerId,
            traceparent = traceparent,
            behandlingsnummer = behandlingsnummer,
        )

    respons.errors?.let {
        throw PdlException("'hentPerson' fra PDL feilet", it)
    }

    logger.trace("Hentet 'hentFoedselsdato' fra PDL")

    return respons
        .data
        ?.hentPerson
        ?.foedselsdato
        ?.firstOrNull()
}