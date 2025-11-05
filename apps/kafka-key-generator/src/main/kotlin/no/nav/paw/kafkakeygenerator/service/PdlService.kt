package no.nav.paw.kafkakeygenerator.service

import kotlinx.coroutines.runBlocking
import no.nav.paw.kafkakeygenerator.exception.IdentitetIkkeFunnetException
import no.nav.paw.kafkakeygenerator.exception.PdlTekniskFeilException
import no.nav.paw.kafkakeygenerator.exception.PdlUkjentIdentitetException
import no.nav.paw.kafkakeygenerator.model.dto.CallId
import no.nav.paw.logging.logger.buildLogger
import no.nav.paw.pdl.PdlClient
import no.nav.paw.pdl.PdlException
import no.nav.paw.pdl.graphql.generated.hentidenter.IdentInformasjon
import no.nav.paw.pdl.hentIdenter
import java.util.*

private const val CONSUMER_ID = "paw-arbeidssoekerregisteret"
private const val PDL_BEHANDLINGSNUMMER = "B452"

class PdlService(
    private val pdlClient: PdlClient
) {
    private val logger = buildLogger

    fun finnIdentiteter(
        identitet: String,
        historikk: Boolean = true,
        callId: CallId = CallId(UUID.randomUUID().toString())
    ): List<IdentInformasjon> = runBlocking {
        try {
            logger.debug("Henter identiteter fra PDL")
            pdlClient.hentIdenter(
                ident = identitet,
                historikk = historikk,
                callId = callId.toString(),
                navConsumerId = CONSUMER_ID,
                behandlingsnummer = PDL_BEHANDLINGSNUMMER
            ) ?: emptyList()
        } catch (exception: PdlException) {
            if (exception.errors?.any { it.message.contains("Fant ikke person") } == true) {
                throw PdlUkjentIdentitetException()
            } else {
                throw PdlTekniskFeilException()
            }
        }
    }
}
