package no.nav.paw.error.handler

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.ContentTransformationException
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.request.RequestAlreadyConsumedException
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import no.nav.paw.error.exception.ClientResponseException
import no.nav.paw.error.exception.ServerResponseException
import no.nav.paw.error.model.ErrorType
import no.nav.paw.error.model.ProblemDetails
import no.nav.paw.error.model.ProblemDetailsBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC

private val logger: Logger = LoggerFactory.getLogger("no.nav.paw.logger.error.http")
private const val MDC_ERROR_ID_KEY = "x_error_id"
private const val MDC_ERROR_TYPE_KEY = "x_error_type"
private const val MDC_EXCEPTION_KEY = "exception"

suspend fun ApplicationCall.handleException(
    throwable: Throwable,
    customResolver: (throwable: Throwable) -> ProblemDetails? = { null }
) {
    val problemDetails = resolveProblemDetails(request, throwable, customResolver)

    MDC.put(MDC_ERROR_ID_KEY, problemDetails.id.toString())
    MDC.put(MDC_ERROR_TYPE_KEY, problemDetails.type.toString())
    MDC.put(MDC_EXCEPTION_KEY, throwable.javaClass.canonicalName)

    logger.error(problemDetails.detail, throwable)

    MDC.remove(MDC_ERROR_ID_KEY)
    MDC.remove(MDC_ERROR_TYPE_KEY)
    MDC.remove(MDC_EXCEPTION_KEY)

    respond(problemDetails.status, problemDetails)
}

fun resolveProblemDetails(
    request: ApplicationRequest,
    throwable: Throwable,
    customResolver: (throwable: Throwable) -> ProblemDetails? = { null }
): ProblemDetails {
    val problemDetails = customResolver(throwable)
    if (problemDetails != null) {
        return problemDetails
    }

    when (throwable) {
        is BadRequestException -> {
            return ProblemDetailsBuilder.builder()
                .type(ErrorType.domain("http").error("kunne-ikke-tolke-forespoersel").build())
                .status(HttpStatusCode.BadRequest)
                .detail("Kunne ikke tolke forespørsel")
                .instance(request.uri)
                .build()
        }

        is ContentTransformationException -> {
            return ProblemDetailsBuilder.builder()
                .type(ErrorType.domain("http").error("kunne-ikke-tolke-innhold").build())
                .status(HttpStatusCode.BadRequest)
                .detail("Kunne ikke tolke innhold i forespørsel")
                .instance(request.uri)
                .build()
        }

        is RequestAlreadyConsumedException -> {
            return ProblemDetailsBuilder.builder()
                .type(ErrorType.domain("http").error("forespoersel-allerede-mottatt").build())
                .status(HttpStatusCode.InternalServerError)
                .detail("Forespørsel er allerede mottatt. Dette er en kodefeil")
                .instance(request.uri)
                .build()
        }

        is ServerResponseException -> {
            return ProblemDetailsBuilder.builder()
                .type(throwable.type)
                .status(throwable.status)
                .detail(throwable.message)
                .instance(request.uri)
                .build()
        }

        is ClientResponseException -> {
            return ProblemDetailsBuilder.builder()
                .type(throwable.type)
                .status(throwable.status)
                .detail(throwable.message)
                .instance(request.uri)
                .build()
        }

        else -> {
            return ProblemDetailsBuilder.builder()
                .detail("Forespørsel feilet med ukjent feil")
                .instance(request.uri)
                .build()
        }
    }
}