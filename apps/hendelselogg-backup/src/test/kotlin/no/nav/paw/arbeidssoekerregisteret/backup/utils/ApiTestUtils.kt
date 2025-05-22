package no.nav.paw.arbeidssoekerregisteret.backup.utils

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.install
import io.ktor.server.routing.IgnoreTrailingSlash
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.paw.arbeidssoekerregisteret.backup.brukerstoette.brukerstoetteRoutes
import no.nav.paw.arbeidssoekerregisteret.backup.brukerstoette.customExceptionResolver
import no.nav.paw.arbeidssoekerregisteret.backup.context.ApplicationContext
import no.nav.paw.error.plugin.installErrorHandlingPlugin
import no.nav.paw.security.authentication.plugin.installAuthenticationPlugin
import no.nav.paw.serialization.jackson.configureJackson
import no.nav.paw.serialization.plugin.installContentNegotiationPlugin

fun ApplicationTestBuilder.configureTestClient(): HttpClient {
    return createClient {
        install(ContentNegotiation) {
            jackson {
                configureJackson()
            }
        }
    }
}

fun ApplicationTestBuilder.configureTestApplication(applicationContext: ApplicationContext) {
    with(applicationContext) {
        application {
            installContentNegotiationPlugin()
            installErrorHandlingPlugin(
                customResolver = customExceptionResolver()
            )
            installAuthenticationPlugin(securityConfig.authProviders)
            install(IgnoreTrailingSlash)
            routing {
                route("/api/v1") {
                    brukerstoetteRoutes(brukerstoetteService)
                }
            }
        }
    }
}
