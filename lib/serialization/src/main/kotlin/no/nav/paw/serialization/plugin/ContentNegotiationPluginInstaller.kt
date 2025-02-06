package no.nav.paw.serialization.plugin

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import no.nav.paw.serialization.jackson.configureJackson

fun Application.installContentNegotiationPlugin(
    configureJackson: ObjectMapper.() -> Unit = ObjectMapper::configureJackson
) {
    install(ContentNegotiation) {
        jackson {
            configureJackson()
        }
    }
}
