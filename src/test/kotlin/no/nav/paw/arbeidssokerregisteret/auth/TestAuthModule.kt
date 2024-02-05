package no.nav.paw.arbeidssokerregisteret.auth

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import no.nav.paw.arbeidssokerregisteret.config.Config
import no.nav.paw.arbeidssokerregisteret.plugins.configureAuthentication
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.security.mock.oauth2.MockOAuth2Server

fun Application.testAuthModule(oAuth2Server: MockOAuth2Server) {
    val config = loadNaisOrLocalConfiguration<Config>("application.yaml")
    val authProviders = config.authProviders.copy(
        tokenx = config.authProviders.tokenx.copy(
            discoveryUrl = oAuth2Server.wellKnownUrl("default").toString(),
            clientId = "default"
        ),
        azure = config.authProviders.azure.copy(
            discoveryUrl = oAuth2Server.wellKnownUrl("default").toString(),
            clientId = "default"
        )
    )
    configureAuthentication(authProviders)
    routing {
        authenticate("tokenx") {
            route("/testAuthTokenx") {
                get {
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
        authenticate("azure") {
            route("/testAuthAzure") {
                get {
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}
