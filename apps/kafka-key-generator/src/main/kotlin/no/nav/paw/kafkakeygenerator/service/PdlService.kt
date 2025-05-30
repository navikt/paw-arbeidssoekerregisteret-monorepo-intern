package no.nav.paw.kafkakeygenerator.service

import no.nav.paw.kafkakeygenerator.vo.CallId
import no.nav.paw.kafkakeygenerator.vo.Either
import no.nav.paw.kafkakeygenerator.vo.Failure
import no.nav.paw.kafkakeygenerator.vo.FailureCode
import no.nav.paw.kafkakeygenerator.vo.GenericFailure
import no.nav.paw.kafkakeygenerator.vo.Identitetsnummer
import no.nav.paw.kafkakeygenerator.vo.flatMap
import no.nav.paw.kafkakeygenerator.vo.left
import no.nav.paw.kafkakeygenerator.vo.mapToFailure
import no.nav.paw.kafkakeygenerator.vo.right
import no.nav.paw.kafkakeygenerator.vo.suspendeableAttempt
import no.nav.paw.pdl.PdlClient
import no.nav.paw.pdl.PdlException
import no.nav.paw.pdl.graphql.generated.enums.IdentGruppe
import no.nav.paw.pdl.graphql.generated.hentidenter.IdentInformasjon
import no.nav.paw.pdl.hentIdenter
import no.nav.paw.pdl.hentIdenterBolk

class PdlService(private val pdlClient: PdlClient) {
    private val consumerId = "paw-arbeidssoekerregisteret"
    private val behandlingsnummer = "B452"

    suspend fun hentIdenter(
        identiteter: List<Identitetsnummer>,
    ): Either<Failure, Map<String, List<IdentInformasjon>>> =
        suspendeableAttempt {
            pdlClient.hentIdenterBolk(
                identer = identiteter.map { it.value },
                grupper = listOf(IdentGruppe.FOLKEREGISTERIDENT),
                historikk = true,
                behandlingsnummer = behandlingsnummer,
                callId = null,
                navConsumerId = null
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
            pdlClient
                .hentIdenter(
                    ident = identitet.value,
                    callId = callId.value,
                    navConsumerId = consumerId,
                    behandlingsnummer = behandlingsnummer,
                    historikk = historikk
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

    suspend fun hentIdentiter(
        callId: CallId,
        identitet: Identitetsnummer,
        historikk: Boolean = false
    ): Either<Failure, List<String>> = hentIdentInformasjon(
        callId = callId,
        identitet = identitet,
        historikk = historikk
    ).map { liste -> liste.map { it.ident } }

    private fun mapPdlException(ex: PdlException): GenericFailure {
        return if (ex.errors?.any { it.message.contains("Fant ikke person") } == true) {
            GenericFailure("pdl", FailureCode.PDL_NOT_FOUND)
        } else {
            GenericFailure("pdl", FailureCode.EXTERNAL_TECHINCAL_ERROR, ex)
        }
    }
}
