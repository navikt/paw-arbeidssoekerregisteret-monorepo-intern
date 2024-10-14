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
import org.slf4j.MDC

private val logger: Logger = LoggerFactory.getLogger("no.nav.paw.logger.error.http")
private const val MDC_ERROR_ID_KEY = "x_error_id"
private const val MDC_ERROR_CODE_KEY = "x_error_code"

suspend fun ApplicationCall.handleException(
    throwable: Throwable,
    resolver: (throwable: Throwable) -> ProblemDetails? = { null }
) {
    val problemDetails = resolveProblemDetails(request, throwable, resolver)
    MDC.put(MDC_ERROR_ID_KEY, problemDetails.id.toString())
    MDC.put(MDC_ERROR_CODE_KEY, problemDetails.code)
    logger.error(problemDetails.detail, throwable)
    MDC.remove(MDC_ERROR_ID_KEY)
    MDC.remove(MDC_ERROR_CODE_KEY)
    respond(problemDetails.status, problemDetails)
}

fun resolveProblemDetails(
    request: ApplicationRequest,
    throwable: Throwable,
    resolver: (throwable: Throwable) -> ProblemDetails? = { null }
): ProblemDetails {
    val problemDetails = resolver(throwable)
    if (problemDetails != null) {
        return problemDetails
    }

    when (throwable) {
        is BadRequestException -> {
            return build400Error(
                code = "PAW_KUNNE_IKKE_TOLKE_FORESPOERSEL",
                detail = "Kunne ikke tolke forespørsel",
                instance = request.uri
            )
        }

        is ContentTransformationException -> {
            return build400Error(
                code = "PAW_KUNNE_IKKE_TOLKE_INNHOLD",
                detail = "Kunne ikke tolke innhold i forespørsel",
                instance = request.uri
            )
        }

        is RequestAlreadyConsumedException -> {
            return build500Error(
                code = "PAW_FORESPOERSEL_ALLEREDE_MOTTATT",
                detail = "Forespørsel er allerede mottatt. Dette er en kodefeil",
                instance = request.uri
            )
        }

        is ServerResponseException -> {
            return buildError(
                code = throwable.code,
                detail = throwable.message,
                status = throwable.status,
                instance = request.uri
            )
        }

        is ClientResponseException -> {
            return buildError(
                code = throwable.code,
                detail = throwable.message,
                status = throwable.status,
                instance = request.uri
            )
        }

        else -> {
            return build500Error(
                code = "PAW_UKJENT_FEIL",
                detail = "Forespørsel feilet med ukjent feil",
                instance = request.uri
            )
        }
    }
}