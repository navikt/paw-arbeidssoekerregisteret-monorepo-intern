package no.nav.paw.dolly.api.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.uri
import io.ktor.server.routing.IgnoreTrailingSlash
import no.nav.paw.error.model.ErrorType
import no.nav.paw.error.model.ProblemDetailsBuilder
import no.nav.paw.error.plugin.ErrorHandlingPlugin

fun Application.configureHTTP() {
    install(IgnoreTrailingSlash)
    install(ErrorHandlingPlugin) {
        customResolver = { throwable, request ->
            when (throwable) {
                is BadRequestException -> {
                    ProblemDetailsBuilder.builder()
                        .type(ErrorType.domain("http").error("kunne-ikke-tolke-forespoersel").build())
                        .status(HttpStatusCode.BadRequest)
                        .detail(throwable.message ?: "Kunne ikke tolke forespørsel")
                        .instance(request.uri)
                        .build()
                }
                else -> null
            }
        }
    }
}