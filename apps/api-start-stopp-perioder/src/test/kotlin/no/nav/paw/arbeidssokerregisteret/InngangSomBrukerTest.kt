package no.nav.paw.arbeidssokerregisteret

import arrow.core.left
import arrow.core.right
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.ApiV1ArbeidssokerPeriodePutRequest
import no.nav.paw.arbeidssokerregisteret.application.*
import no.nav.paw.arbeidssokerregisteret.application.regler.IkkeAnsattOgForhaandsgodkjentAvAnsatt
import no.nav.paw.arbeidssokerregisteret.auth.configureAuthentication
import no.nav.paw.arbeidssokerregisteret.domain.Identitetsnummer
import no.nav.paw.arbeidssokerregisteret.plugins.configureHTTP
import no.nav.paw.arbeidssokerregisteret.plugins.configureSerialization
import no.nav.paw.arbeidssokerregisteret.routes.arbeidssokerRoutes
import no.nav.security.mock.oauth2.MockOAuth2Server

class InngangSomBrukerTest : FreeSpec({
    val oauth = MockOAuth2Server()

    beforeSpec {
        oauth.start()
    }

    afterSpec {
        oauth.shutdown()
    }
    "Teste inngang som bruker" - {

        "På vegne av seg selv" - {
            val startStoppRequestHandler: StartStoppRequestHandler = mockk()
            coEvery {
                with(any<RequestScope>()) {
                    startStoppRequestHandler.startArbeidssokerperiode(any(), any())
                }
            } returns OK(
                regel = Regel(
                    id = Over18AarOgBosattEtterFregLoven,
                    beskrivelse = "",
                    opplysninger = emptyList(),
                    vedTreff = ::ok
                ),
                opplysning = emptySet()
            ).right()

            testApplication {
                application {
                    configureHTTP()
                    configureSerialization()
                    configureAuthentication(oauth)
                    routing {
                        authenticate("tokenx") {
                            arbeidssokerRoutes(startStoppRequestHandler, mockk())
                        }
                    }
                }
                val token = oauth.issueToken(
                    claims = mapOf(
                        "acr" to "idporten-loa-high",
                        "pid" to "12345678909"
                    )
                )
                val client = createClient {
                    install(ContentNegotiation) {
                        jackson {
                            registerKotlinModule()
                            registerModule(JavaTimeModule())
                        }
                    }
                }
                val response = client.put("/api/v1/arbeidssoker/periode") {
                    bearerAuth(token.serialize())
                    headers { append(HttpHeaders.ContentType, ContentType.Application.Json) }
                    setBody(
                        ApiV1ArbeidssokerPeriodePutRequest(
                            identitetsnummer = "12345678909",
                            registreringForhaandsGodkjentAvAnsatt = false,
                            periodeTilstand = ApiV1ArbeidssokerPeriodePutRequest.PeriodeTilstand.STARTET
                        )
                    )
                }
                response.status shouldBe HttpStatusCode.NoContent
                coVerify(exactly = 1) {
                    with(any<RequestScope>()) {
                        startStoppRequestHandler.startArbeidssokerperiode(Identitetsnummer("12345678909"), false)
                    }
                }
            }
        }


        "Bruker som har forhandsgodkjentflagg aktivt" {
            val startStoppRequestHandler: StartStoppRequestHandler = mockk()
            coEvery {
                with(any<RequestScope>()) {
                    startStoppRequestHandler.startArbeidssokerperiode(any(), any())
                }
            } returns Problem(
                regel = Regel(
                    id = IkkeAnsattOgForhaandsgodkjentAvAnsatt,
                    beskrivelse = "",
                    opplysninger = emptyList(),
                    vedTreff = ::problem
                ),
                opplysning = emptySet()
            ).left()
            testApplication {
                application {
                    configureHTTP()
                    configureSerialization()
                    configureAuthentication(oauth)
                    routing {
                        authenticate("tokenx") {
                            arbeidssokerRoutes(startStoppRequestHandler, mockk())
                        }
                    }
                }
                val token = oauth.issueToken(
                    claims = mapOf(
                        "acr" to "idporten-loa-high",
                        "pid" to "12345678909"
                    )
                )
                val client = createClient {
                    install(ContentNegotiation) {
                        jackson {
                            registerKotlinModule()
                            registerModule(JavaTimeModule())
                        }
                    }
                }
                val response = client.put("/api/v1/arbeidssoker/periode") {
                    bearerAuth(token.serialize())
                    headers { append(HttpHeaders.ContentType, ContentType.Application.Json) }
                    setBody(
                        ApiV1ArbeidssokerPeriodePutRequest(
                            identitetsnummer = "12345678909",
                            registreringForhaandsGodkjentAvAnsatt = false,
                            periodeTilstand = ApiV1ArbeidssokerPeriodePutRequest.PeriodeTilstand.STARTET
                        )
                    )
                }
                response.status shouldBe HttpStatusCode.BadRequest
                coVerify(exactly = 1) {
                    with(any<RequestScope>()) {
                        startStoppRequestHandler.startArbeidssokerperiode(Identitetsnummer("12345678909"), false)
                    }
                }
            }
        }
    }
})
