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
import no.nav.paw.arbeidssokerregisteret.application.Over18AarOgBosattEtterFregLoven
import no.nav.paw.arbeidssokerregisteret.application.Regel
import no.nav.paw.arbeidssokerregisteret.application.StartStoppRequestHandler
import no.nav.paw.arbeidssokerregisteret.application.grunnlagForGodkjenning
import no.nav.paw.arbeidssokerregisteret.application.regler.IkkeAnsattOgIkkeSystemOgForhaandsgodkjent
import no.nav.paw.arbeidssokerregisteret.application.skalAvises
import no.nav.paw.arbeidssokerregisteret.auth.configureAuthentication
import no.nav.paw.arbeidssokerregisteret.plugins.configureHTTP
import no.nav.paw.arbeidssokerregisteret.plugins.configureSerialization
import no.nav.paw.arbeidssokerregisteret.routes.arbeidssokerRoutesV2
import no.nav.paw.felles.collection.pawNonEmptyListOf
import no.nav.paw.felles.model.Identitetsnummer
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
                startStoppRequestHandler.startArbeidssokerperiode(any(), any(), any(), any())
            } returns GrunnlagForGodkjenning(
                regel = Regel(
                    id = Over18AarOgBosattEtterFregLoven,
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
                        authenticate("tokenx") {
                            arbeidssokerRoutesV2(startStoppRequestHandler)
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
                val response = client.put("/api/v2/arbeidssoker/periode") {
                    bearerAuth(token.serialize())
                    headers { append(HttpHeaders.ContentType, ContentType.Application.Json) }
                    setBody(
                        ApiV2ArbeidssokerPeriodePutRequest(
                            identitetsnummer = "12345678909",
                            registreringForhaandsGodkjentAvAnsatt = false,
                            periodeTilstand = ApiV2ArbeidssokerPeriodePutRequest.PeriodeTilstand.STARTET
                        )
                    )
                }
                response.status shouldBe HttpStatusCode.NoContent
                coVerify(exactly = 1) {
                    startStoppRequestHandler.startArbeidssokerperiode(
                        any(),
                        Identitetsnummer("12345678909"), false, null
                    )
                }
            }
        }


        "Bruker som har forhandsgodkjentflagg aktivt" {
            val startStoppRequestHandler: StartStoppRequestHandler = mockk()
            coEvery {
                startStoppRequestHandler.startArbeidssokerperiode(any(), any(), any(), any())
            } returns skalAvises(
                regel = Regel(
                    id = IkkeAnsattOgIkkeSystemOgForhaandsgodkjent,
                    kritierier = emptyList(),
                    vedTreff = ::skalAvises
                ),
                opplysninger = emptySet()
            ).mapLeft { pawNonEmptyListOf(it) }
            testApplication {
                application {
                    configureHTTP()
                    configureSerialization()
                    configureAuthentication(oauth)
                    routing {
                        authenticate("tokenx") {
                            arbeidssokerRoutesV2(startStoppRequestHandler)
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
                val response = client.put("/api/v2/arbeidssoker/periode") {
                    bearerAuth(token.serialize())
                    headers { append(HttpHeaders.ContentType, ContentType.Application.Json) }
                    setBody(
                        ApiV2ArbeidssokerPeriodePutRequest(
                            identitetsnummer = "12345678909",
                            registreringForhaandsGodkjentAvAnsatt = false,
                            periodeTilstand = ApiV2ArbeidssokerPeriodePutRequest.PeriodeTilstand.STARTET
                        )
                    )
                }
                response.status shouldBe HttpStatusCode.BadRequest
                coVerify(exactly = 1) {
                    startStoppRequestHandler.startArbeidssokerperiode(
                        any(),
                        Identitetsnummer("12345678909"),
                        false,
                        null
                    )
                }
            }
        }
    }
})
