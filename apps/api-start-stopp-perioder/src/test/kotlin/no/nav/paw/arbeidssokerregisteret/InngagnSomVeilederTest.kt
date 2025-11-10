package no.nav.paw.arbeidssokerregisteret

import arrow.core.right
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.headers
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.append
import io.ktor.serialization.jackson.jackson
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.ApiV2ArbeidssokerPeriodePutRequest
import no.nav.paw.arbeidssokerregisteret.application.GrunnlagForGodkjenning
import no.nav.paw.arbeidssokerregisteret.application.Regel
import no.nav.paw.arbeidssokerregisteret.application.StartStoppRequestHandler
import no.nav.paw.arbeidssokerregisteret.application.grunnlagForGodkjenning
import no.nav.paw.arbeidssokerregisteret.application.regler.AnsattHarTilgangTilBruker
import no.nav.paw.arbeidssokerregisteret.auth.configureAuthentication
import no.nav.paw.arbeidssokerregisteret.plugins.configureHTTP
import no.nav.paw.arbeidssokerregisteret.plugins.configureSerialization
import no.nav.paw.arbeidssokerregisteret.routes.arbeidssokerRoutesV2
import no.nav.paw.felles.model.Identitetsnummer
import no.nav.security.mock.oauth2.MockOAuth2Server

class InngagnSomVeilederTest : FreeSpec({
    val oauth = MockOAuth2Server()

    beforeSpec {
        oauth.start()
    }

    afterSpec {
        oauth.shutdown()
    }

    "inngang som veileder" - {
        "forhåndsgodkjent param skal taes med til validering" {
            val startStoppRequestHandler: StartStoppRequestHandler = mockk()
            coEvery {
                startStoppRequestHandler.startArbeidssokerperiode(any(), any(), any(), any())
            } returns GrunnlagForGodkjenning(
                regel = Regel(
                    id = AnsattHarTilgangTilBruker,
                    kritierier = emptyList(),
                    vedTreff = ::grunnlagForGodkjenning
                ),
                opplysning = emptySet()
            ).right()
            testApplication {
                application {
                    configureHTTP()
                    configureSerialization()
                    configureAuthentication(oauth)
                    routing {
                        authenticate("tokenx", "azure") {
                            arbeidssokerRoutesV2(startStoppRequestHandler)
                        }
                    }
                }
                val tokenMap = mapOf(
                    "oid" to "989f736f-14db-45dc-b8d1-94d621dbf2bb",
                    "NAVident" to "test"
                )
                val token = oauth.issueToken(
                    claims = tokenMap
                )
                val client = createClient {
                    install(ContentNegotiation) {
                        jackson {
                            registerKotlinModule()
                            registerModule(JavaTimeModule())
                        }
                    }
                }
                val response = client.put("/api/v2/arbeidssoker/periode") {
                    bearerAuth(token.serialize())
                    headers {
                        append(HttpHeaders.ContentType, ContentType.Application.Json)
                    }
                    setBody(
                        ApiV2ArbeidssokerPeriodePutRequest(
                            identitetsnummer = "12345678909",
                            registreringForhaandsGodkjentAvAnsatt = true,
                            periodeTilstand = ApiV2ArbeidssokerPeriodePutRequest.PeriodeTilstand.STARTET
                        )
                    )
                }
                response.status shouldBe HttpStatusCode.NoContent
                coVerify(exactly = 1) {
                    startStoppRequestHandler.startArbeidssokerperiode(
                        any(),
                        Identitetsnummer("12345678909"),
                        true,
                        null
                    )
                }

                val response2 = client.put("/api/v2/arbeidssoker/periode") {
                    bearerAuth(token.serialize())
                    headers {
                        append(HttpHeaders.ContentType, ContentType.Application.Json)
                    }
                    setBody(
                        ApiV2ArbeidssokerPeriodePutRequest(
                            identitetsnummer = "12345678909",
                            registreringForhaandsGodkjentAvAnsatt = false,
                            periodeTilstand = ApiV2ArbeidssokerPeriodePutRequest.PeriodeTilstand.STARTET
                        )
                    )
                }
                response2.status shouldBe HttpStatusCode.NoContent
                coVerify(exactly = 1) {
                    startStoppRequestHandler.startArbeidssokerperiode(
                        any(),
                        Identitetsnummer("12345678909"), false, null
                    )
                }
            }
        }
    }
})
