package no.nav.paw.error.handler

import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.ContentTransformationException
import io.ktor.server.request.RequestAlreadyConsumedException
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import no.nav.paw.error.exception.ClientResponseException
import no.nav.paw.error.exception.ServerResponseException
import no.nav.paw.error.model.build400Error
import no.nav.paw.error.model.build500Error
import no.nav.paw.error.model.buildError
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private const val ERROR_TYPE_PREFIX = "PAW_"
private val logger: Logger = LoggerFactory.getLogger("no.nav.paw.logger.error.http")

suspend fun <T : Throwable> ApplicationCall.handleException(throwable: T) {
    when (throwable) {
        is ContentTransformationException -> {
            val error = build400Error(
                "${ERROR_TYPE_PREFIX}KUNNE_IKKE_TOLKE_INNHOLD",
                "Kunne ikke tolke innhold i kall",
                this.request.uri
            )
            logger.debug(error.detail, throwable)
            this.respond(error.status, error)
        }

        is ClientResponseException -> {
            val error = buildError(
                "${ERROR_TYPE_PREFIX}${throwable.code}",
                throwable.message,
                throwable.status,
                this.request.uri
            )
            logger.warn(error.detail, throwable)
            this.respond(error.status, error)
        }

        is ServerResponseException -> {
            val error = buildError(
                "${ERROR_TYPE_PREFIX}${throwable.code}",
                throwable.message,
                throwable.status,
                this.request.uri
            )
            logger.error(error.detail, throwable)
            this.respond(error.status, error)
        }

        is BadRequestException -> {
            val error =
                build400Error(
                    "${ERROR_TYPE_PREFIX}ULOVLIG_FORESPOERSEL",
                    "Kunne ikke tolke innhold i forespørsel",
                    this.request.uri
                )
            logger.error(error.detail, throwable)
            this.respond(error.status, error)
        }

        is RequestAlreadyConsumedException -> {
            val error = build500Error(
                "${ERROR_TYPE_PREFIX}FORESPOERSEL_ALLEREDE_MOTTATT",
                "Forespørsel er allerede mottatt. Dette er en kodefeil",
                this.request.uri
            )
            logger.error(error.detail, throwable)
            this.respond(error.status, error)
        }

        else -> {
            val error = build500Error(
                "${ERROR_TYPE_PREFIX}UKJENT_FEIL",
                "Forespørsel feilet med ukjent feil",
                this.request.uri
            )
            logger.error(error.detail, throwable)
            this.respond(error.status, error)
        }
    }
}