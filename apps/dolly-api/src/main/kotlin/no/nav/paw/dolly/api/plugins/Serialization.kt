package no.nav.paw.dolly.api.plugins

import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import no.nav.paw.client.factory.configureJackson

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        jackson {
            configureJackson()
        }
    }
}