package no.nav.paw.arbeidssokerregisteret.services

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import kotlinx.coroutines.channels.ClosedReceiveChannelException
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
    suspend fun hentPersonInfo(requestScope: RequestScope, ident: String): Person? =
        try {
            pdlClient.hentPerson(
                ident = ident,
                callId = requestScope.callId,
                traceparent = requestScope.traceparent,
                navConsumerId = requestScope.navConsumerId,
                behandlingsnummer = BEHANDLINGSNUMMER
            )
        } catch (ex: PdlException) {
            throw RemoteServiceException(
                description = "Feil ved henting av personinfo fra PDL",
                feilkode = InternFeilkode.UVENTET_FEIL_MOT_EKSTERN_TJENESTE,
                causedBy = ex
            )
        } catch (ex: ClosedReceiveChannelException) {
            throw RemoteServiceException(
                description = "Feil ved henting av personinfo fra PDL",
                feilkode = InternFeilkode.UVENTET_FEIL_MOT_EKSTERN_TJENESTE,
                causedBy = ex
            )
        }
}
