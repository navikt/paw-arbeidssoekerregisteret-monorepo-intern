package no.nav.paw.arbeidssoekerregisteret.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import no.nav.paw.arbeidssoekerregisteret.plugins.tracing.OpenTelemetryPlugin

fun Application.configureTracing() {
    install(OpenTelemetryPlugin)
}