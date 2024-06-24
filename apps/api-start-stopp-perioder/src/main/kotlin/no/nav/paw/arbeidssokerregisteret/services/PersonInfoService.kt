package no.nav.paw.arbeidssokerregisteret.services

import no.nav.paw.arbeidssokerregisteret.RequestScope
import no.nav.paw.arbeidssokerregisteret.plugins.InternFeilkode
import no.nav.paw.pdl.PdlClient
import no.nav.paw.pdl.PdlException
import no.nav.paw.pdl.graphql.generated.hentperson.Person
import no.nav.paw.pdl.hentPerson

const val BEHANDLINGSNUMMER = "B452"

class PersonInfoService(
    private val pdlClient: PdlClient
) {
    context(RequestScope)
    suspend fun hentPersonInfo(ident: String): Person? =
        try {
            pdlClient.hentPerson(
                ident = ident,
                callId = callId,
                traceparent = traceparent,
                navConsumerId = navConsumerId,
                behandlingsnummer = BEHANDLINGSNUMMER
            )
        } catch (ex: PdlException) {
            throw RemoteServiceException(
                description = "Feil ved henting av personinfo fra PDL",
                feilkode = InternFeilkode.UVENTET_FEIL_MOT_EKSTERN_TJENESTE,
                causedBy = ex
            )
        }
}
