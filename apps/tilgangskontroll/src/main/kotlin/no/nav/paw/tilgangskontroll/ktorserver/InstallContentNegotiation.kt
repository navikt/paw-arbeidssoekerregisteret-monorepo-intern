package no.nav.paw.tilgangskontroll.ktorserver

import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import no.nav.paw.tilgangskontroll.utils.configureJackson

fun Application.installContentNegotiation() {
    install(ContentNegotiation) {
        jackson {
            configureJackson()
        }
    }
}