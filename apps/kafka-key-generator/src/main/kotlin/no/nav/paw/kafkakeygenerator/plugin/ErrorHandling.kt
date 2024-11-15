package no.nav.paw.kafkakeygenerator.plugin

import com.fasterxml.jackson.databind.DatabindException
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.path
import io.ktor.server.response.respondText
import no.nav.paw.kafkakeygenerator.utils.masker
import org.slf4j.LoggerFactory

private val feilLogger = LoggerFactory.getLogger("error_logger")

fun Application.configureErrorHandling() {
    install(StatusPages) {
        exception<Throwable> { call, throwable ->
            when (throwable) {
                is DatabindException -> {
                    feilLogger.info(
                        "Ugyldig kall {}, feilet, grunnet: {}",
                        masker(call.request.path()),
                        masker(throwable.message)
                    )
                    call.respondText(
                        "Bad request",
                        ContentType.Text.Plain,
                        HttpStatusCode.BadRequest
                    )
                }

                else -> {
                    feilLogger.error(
                        "Kall {}, feilet, grunnet: {}",
                        masker(call.request.path()),
                        masker(throwable.message)
                    )
                    call.respondText(
                        "En uventet feil oppstod",
                        ContentType.Text.Plain,
                        HttpStatusCode.InternalServerError
                    )
                }
            }
        }
    }
}
