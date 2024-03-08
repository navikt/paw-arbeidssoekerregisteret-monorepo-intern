package no.nav.paw.arbeidssokerregisteret

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.paw.arbeidssokerregisteret.application.*
import no.nav.paw.arbeidssokerregisteret.domain.Feilkode
import no.nav.paw.arbeidssokerregisteret.domain.http.*
import no.nav.paw.arbeidssokerregisteret.plugins.configureHTTP
import no.nav.paw.arbeidssokerregisteret.plugins.configureSerialization
import no.nav.paw.arbeidssokerregisteret.routes.arbeidssokerRoutes

class ApplicationOpplysningerTest : FunSpec({
    test("Verifiser opplysninger happy path") {
        testApplication {
            application {
                configureSerialization()
                configureHTTP()
            }
            routing {
                val requestHandler = mockk<RequestHandler>()
                coEvery {
                    with(any<RequestScope>()) {
                        requestHandler.opprettBrukeropplysninger(any())
                    }
                } returns Right(ValidationResultOk)
                arbeidssokerRoutes(requestHandler)
            }
            val client = createClient {
                install(ContentNegotiation) {
                    jackson {
                        registerKotlinModule()
                        registerModule(JavaTimeModule())
                    }
                }
            }
            val response = client.post("/api/v1/arbeidssoker/opplysninger") {
                headers {
                    append(HttpHeaders.ContentType, ContentType.Application.Json)
                }
                setBody(
                    """
                {
                    "identitetsnummer":"12345678900",
                    "opplysningerOmArbeidssoeker":{
                        "utdanning":{
                            "nus":"0"},
                            "helse":{
                                "helsetilstandHindrerArbeid":"NEI"
                            },
                            "jobbsituasjon":{
                                "beskrivelser":[
                                    {
                                        "beskrivelse":"HAR_SAGT_OPP",
                                        "detaljer":{
                                            "stilling":"Bilskadereparatør",
                                            "stilling_styrk08":"7213"
                                        }
                                    }
                                ]
                            }
                            ,"annet":{
                                "andreForholdHindrerArbeid":"NEI"
                            }
                        }
                    }
""".trimIndent()
                )
            }
            response.status shouldBe HttpStatusCode.Accepted
        }
    }
})


