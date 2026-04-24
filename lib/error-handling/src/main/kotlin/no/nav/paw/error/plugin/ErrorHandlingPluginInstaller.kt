package no.nav.paw.error.plugin

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.request.ApplicationRequest
import no.nav.paw.error.model.ProblemDetails

fun Application.installErrorHandlingPlugin(
    customResolver: (throwable: Throwable, request: ApplicationRequest) -> ProblemDetails? = { _, _ -> null }
) {
    install(ErrorHandlingPlugin) {
        this.customResolver = customResolver
    }
}
