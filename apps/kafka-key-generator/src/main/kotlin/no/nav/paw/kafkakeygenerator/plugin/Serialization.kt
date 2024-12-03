package no.nav.paw.kafkakeygenerator.plugin

import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation

fun Application.configSerialization() {
    install(ContentNegotiation) {
        jackson()
    }
}
