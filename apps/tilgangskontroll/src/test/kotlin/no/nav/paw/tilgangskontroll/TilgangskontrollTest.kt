package no.nav.paw.tilgangskontroll

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.nimbusds.jwt.SignedJWT
import io.kotest.common.runBlocking
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.call.body
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.append
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.error.model.Data
import no.nav.paw.tilgangskontroll.api.models.TilgangskontrollRequestV1
import no.nav.paw.tilgangskontroll.api.models.TilgangskontrollResponseV1
import no.nav.paw.tilgangskontroll.client.TilgangskontrollClientConfig
import no.nav.paw.tilgangskontroll.client.tilgangsTjenesteForAnsatte
import no.nav.paw.tilgangskontroll.ktorserver.AuthProvider
import no.nav.paw.tilgangskontroll.ktorserver.AuthProviderConfig
import no.nav.paw.tilgangskontroll.ktorserver.AuthProviders
import no.nav.paw.tilgangskontroll.ktorserver.configureAuthentication
import no.nav.paw.tilgangskontroll.ktorserver.installContentNegotiation
import no.nav.paw.tilgangskontroll.ktorserver.installStatusPage
import no.nav.paw.tilgangskontroll.routes.apiV1Tilgang
import no.nav.paw.tilgangskontroll.vo.EntraId
import no.nav.paw.tilgangskontroll.vo.Identitetsnummer
import no.nav.paw.tilgangskontroll.vo.Tilgang
import no.nav.security.mock.oauth2.MockOAuth2Server
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class TilgangskontrollTest: FreeSpec({
    val mockOAuthServer = MockOAuth2Server()
    beforeSpec {
        mockOAuthServer.start()
    }
    afterSpec {
        mockOAuthServer.shutdown()
    }
    val map = ConcurrentHashMap<Triple<EntraId, Identitetsnummer, Tilgang>, Boolean>()
    val service = tilgangsTjenesteMock(map)

    "Verifiser applikasjonsflyt".config(enabled = true) {
        val ansatt = NavAnsatt(UUID.randomUUID(), "Z123")
        val person = Identitetsnummer("12345678901")
        val token = mockOAuthServer.ansattToken(ansatt)
        map[Triple(EntraId(ansatt.azureId), person, Tilgang.LESE)] = true
        map[Triple(EntraId(ansatt.azureId), person, Tilgang.SKRIVE)] = false
        testApplication {
            application {
                configureAuthentication(mockOAuthServer, AuthProvider.EntraId)
                installStatusPage()
                installContentNegotiation()
                routing {
                    authenticate(AuthProvider.EntraId.name) {
                        apiV1Tilgang(service)
                    }
                }
            }
            val client = createClient {
                install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                    jackson {
                        registerKotlinModule()
                        registerModule(JavaTimeModule())
                    }
                }
            }
            val tilgangskontrollKlient = tilgangsTjenesteForAnsatte(
                httpClient = client,
                config = TilgangskontrollClientConfig(
                    uri = "",
                    scope = "MOCK"
                ),
                tokenProvider = { token.serialize() }
            )
            tilgangskontrollKlient.harAnsattTilgangTilPerson(
                navIdent = no.nav.paw.model.EntraId(ansatt.azureId),
                identitetsnummer = no.nav.paw.model.Identitetsnummer(person.value),
                tilgang = no.nav.paw.tilgangskontroll.client.Tilgang.LESE
            ) should { response ->
                response.shouldBeInstanceOf<Data<Boolean>>()
                response.data shouldBe true
            }
            tilgangskontrollKlient.harAnsattTilgangTilPerson(
                navIdent = no.nav.paw.model.EntraId(ansatt.azureId),
                identitetsnummer = no.nav.paw.model.Identitetsnummer(person.value),
                tilgang = no.nav.paw.tilgangskontroll.client.Tilgang.SKRIVE
            ) should { response ->
                response.shouldBeInstanceOf<Data<Boolean>>()
                response.data shouldBe false
            }
        }
    }

})

private fun tilgangsTjenesteMock(map: ConcurrentHashMap<Triple<EntraId, Identitetsnummer, Tilgang>, Boolean>) =
    object : TilgangsTjenesteForAnsatte {
        override suspend fun harAnsattTilgangTilPerson(
            navIdent: EntraId,
            identitetsnummer: Identitetsnummer,
            tilgang: Tilgang
        ): Boolean {
            return map[Triple(navIdent, identitetsnummer, tilgang)] ?: false
        }
    }


fun Application.configureAuthentication(
    oAuth2Server: MockOAuth2Server,
    vararg authProvider: AuthProvider
) {
    val authProviders = authProvider.map { provider ->
        loadNaisOrLocalConfiguration<AuthProviderConfig>(provider.config)
            .copy(
                discoveryUrl = oAuth2Server.wellKnownUrl("default").toString(),
                clientId = "default"
            )
    } .let(::AuthProviders)
    configureAuthentication(authProviders)
}

fun MockOAuth2Server.ansattToken(navAnsatt: NavAnsatt): SignedJWT = issueToken(
    claims = mapOf(
        "oid" to navAnsatt.azureId,
        "NAVident" to navAnsatt.ident
    )
)

data class NavAnsatt(
    val azureId: UUID,
    val ident: String
)
