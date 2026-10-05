package no.nav.paw.serialization.plugin

import io.ktor.serialization.jackson3.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import no.nav.paw.serialization.jackson.configureJackson as defaultConfigureJackson
import tools.jackson.databind.json.JsonMapper

fun Application.installContentNegotiationPlugin(
    configureJackson: JsonMapper.Builder.() -> Unit = { defaultConfigureJackson() }
) {
    install(ContentNegotiation) {
        jackson {
            configureJackson()
        }
    }
}
