package no.nav.paw.kafkakeygenerator.service

import no.nav.paw.kafkakeygenerator.exception.PdlIdentiteterIkkeFunnetException
import no.nav.paw.kafkakeygenerator.exception.PdlTekniskFeilException
import no.nav.paw.kafkakeygenerator.model.CallId
import no.nav.paw.kafkakeygenerator.model.Either
import no.nav.paw.kafkakeygenerator.model.Failure
import no.nav.paw.kafkakeygenerator.model.FailureCode
import no.nav.paw.kafkakeygenerator.model.GenericFailure
import no.nav.paw.kafkakeygenerator.model.Identitetsnummer
import no.nav.paw.kafkakeygenerator.model.flatMap
import no.nav.paw.kafkakeygenerator.model.left
import no.nav.paw.kafkakeygenerator.model.mapToFailure
import no.nav.paw.kafkakeygenerator.model.right
import no.nav.paw.kafkakeygenerator.model.suspendeableAttempt
import no.nav.paw.pdl.PdlClient
import no.nav.paw.pdl.PdlException
import no.nav.paw.pdl.graphql.generated.enums.IdentGruppe
import no.nav.paw.pdl.graphql.generated.hentidenter.IdentInformasjon
import no.nav.paw.pdl.hentIdenter
import no.nav.paw.pdl.hentIdenterBolk
import java.util.*

private const val CONSUMER_ID = "paw-arbeidssoekerregisteret"
private const val PDL_BEHANDLINGSNUMMER = "B452"

class PdlService(private val pdlClient: PdlClient) {

    suspend fun finnIdentiteter(
        identitet: String,
        historikk: Boolean = true,
        callId: UUID = UUID.randomUUID()
    ): List<IdentInformasjon> {
        try {
            return pdlClient.hentIdenter(
                ident = identitet,
                historikk = historikk,
                callId = callId.toString(),
                navConsumerId = CONSUMER_ID,
                behandlingsnummer = PDL_BEHANDLINGSNUMMER
            ) ?: emptyList()
        } catch (exception: PdlException) {
            if (exception.errors?.any { it.message.contains("Fant ikke person") } == true) {
                throw PdlIdentiteterIkkeFunnetException()
            } else {
                throw PdlTekniskFeilException()
            }
        }
    }

    suspend fun hentIdenter(
        identiteter: List<Identitetsnummer>,
        historikk: Boolean = true,
        callId: UUID = UUID.randomUUID()
    ): Either<Failure, Map<String, List<IdentInformasjon>>> =
        suspendeableAttempt {
            pdlClient.hentIdenterBolk(
                identer = identiteter.map { it.value },
                grupper = listOf(IdentGruppe.FOLKEREGISTERIDENT),
                historikk = historikk,
                callId = callId.toString(),
                navConsumerId = CONSUMER_ID,
                behandlingsnummer = PDL_BEHANDLINGSNUMMER
            )
        }.mapToFailure { exception ->
            when (exception) {
                is PdlException -> mapPdlException(exception)
                else -> GenericFailure("pdl", FailureCode.EXTERNAL_TECHINCAL_ERROR, exception)
            }
        }

    suspend fun hentIdentInformasjon(
        callId: CallId,
        identitet: Identitetsnummer,
        historikk: Boolean = false
    ): Either<Failure, List<IdentInformasjon>> {
        return suspendeableAttempt {
            pdlClient.hentIdenter(
                ident = identitet.value,
                historikk = historikk,
                callId = callId.value,
                navConsumerId = CONSUMER_ID,
                behandlingsnummer = PDL_BEHANDLINGSNUMMER
            )
        }.mapToFailure { exception ->
            when (exception) {
                is PdlException -> mapPdlException(exception)
                else -> GenericFailure("pdl", FailureCode.EXTERNAL_TECHINCAL_ERROR, exception)
            }
        }.flatMap { hits ->
            if (hits.isNullOrEmpty()) {
                left(GenericFailure("pdl", FailureCode.PDL_NOT_FOUND))
            } else {
                right(hits)
            }
        }
    }

    private fun mapPdlException(ex: PdlException): GenericFailure {
        return if (ex.errors?.any { it.message.contains("Fant ikke person") } == true) {
            GenericFailure("pdl", FailureCode.PDL_NOT_FOUND)
        } else {
            GenericFailure("pdl", FailureCode.EXTERNAL_TECHINCAL_ERROR, ex)
        }
    }
}
