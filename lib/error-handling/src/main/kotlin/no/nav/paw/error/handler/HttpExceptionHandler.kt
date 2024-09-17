package no.nav.paw.error.handler

import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.ContentTransformationException
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.request.RequestAlreadyConsumedException
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import no.nav.paw.error.exception.ClientResponseException
import no.nav.paw.error.exception.ServerResponseException
import no.nav.paw.error.model.ProblemDetails
import no.nav.paw.error.model.build400Error
import no.nav.paw.error.model.build500Error
import no.nav.paw.error.model.buildError
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private const val ERROR_TYPE_PREFIX = "PAW_"
private val logger: Logger = LoggerFactory.getLogger("no.nav.paw.logger.error.http")

suspend fun ApplicationCall.handleException(throwable: Throwable) {
    val problemDetails = resolveProblemDetails(request, throwable)
    logger.error(problemDetails.detail, throwable)
    respond(problemDetails.status, problemDetails)
}

fun resolveProblemDetails(request: ApplicationRequest, throwable: Throwable): ProblemDetails {
    when (throwable) {
        is BadRequestException -> {
            return build400Error(
                "${ERROR_TYPE_PREFIX}KUNNE_IKKE_TOLKE_FORESPOERSEL",
                "Kunne ikke tolke innhold i forespørsel",
                request.uri
            )
        }

        is ContentTransformationException -> {
            return build400Error(
                "${ERROR_TYPE_PREFIX}KUNNE_IKKE_TOLKE_INNHOLD",
                "Kunne ikke tolke innhold i kall",
                request.uri
            )
        }

        is RequestAlreadyConsumedException -> {
            return build500Error(
                "${ERROR_TYPE_PREFIX}FORESPOERSEL_ALLEREDE_MOTTATT",
                "Forespørsel er allerede mottatt. Dette er en kodefeil",
                request.uri
            )
        }

        is ServerResponseException -> {
            return buildError(
                "${ERROR_TYPE_PREFIX}${throwable.code}",
                throwable.message,
                throwable.status,
                request.uri
            )
        }

        is ClientResponseException -> {
            return buildError(
                "${ERROR_TYPE_PREFIX}${throwable.code}",
                throwable.message,
                throwable.status,
                request.uri
            )
        }

        else -> {
            return build500Error(
                "${ERROR_TYPE_PREFIX}UKJENT_FEIL",
                "Forespørsel feilet med ukjent feil",
                request.uri
            )
        }
    }
}