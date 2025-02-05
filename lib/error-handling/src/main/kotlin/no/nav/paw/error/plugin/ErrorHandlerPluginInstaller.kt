package no.nav.paw.error.plugin

import io.ktor.server.application.Application
import io.ktor.server.application.install
import no.nav.paw.error.model.ProblemDetails

fun Application.installErrorHandling(
    customResolver: (throwable: Throwable) -> ProblemDetails? = { null }
) {
    install(ErrorHandlingPlugin) {
        this.customResolver = customResolver
    }
}
