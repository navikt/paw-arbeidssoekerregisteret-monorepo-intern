package no.nav.paw.arbeidssokerregisteret

import arrow.core.left
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.*
import no.nav.paw.arbeidssokerregisteret.application.*
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.DomeneOpplysning
import no.nav.paw.arbeidssokerregisteret.plugins.configureHTTP
import no.nav.paw.arbeidssokerregisteret.plugins.configureSerialization
import no.nav.paw.arbeidssokerregisteret.routes.arbeidssokerRoutes

class ApplicationPeriodeTest : FunSpec({
    test("Verifiser at vi returnerer 'Feil' objekt når vi avviser en periode") {
        testApplication {
            application {
                configureSerialization()
                configureHTTP()
            }
            routing {
                val startStoppRequestHandler = mockk<StartStoppRequestHandler>()
                coEvery {
                    with(any<RequestScope>()) {
                        startStoppRequestHandler.startArbeidssokerperiode(any(), any())
                    }
                } returns Problem(
                    regel = Regel(
                        id = Under18Aar,
                        opplysninger = listOf(DomeneOpplysning.ErUnder18Aar),
                        vedTreff = ::problem
                    ),
                    opplysning = listOf(DomeneOpplysning.ErUnder18Aar, DomeneOpplysning.BosattEtterFregLoven)
                ).left()
                arbeidssokerRoutes(startStoppRequestHandler, mockk())
            }
            val client = createClient {
                install(ContentNegotiation) {
                    jackson {
                        registerKotlinModule()
                        registerModule(JavaTimeModule())
                    }
                }
            }
            val response = client.put("/api/v1/arbeidssoker/periode") {
                headers {
                    append(HttpHeaders.ContentType, ContentType.Application.Json)
                }
                setBody(
                    ApiV1ArbeidssokerPeriodePutRequest(
                        identitetsnummer = "12345678911",
                        periodeTilstand = ApiV1ArbeidssokerPeriodePutRequest.PeriodeTilstand.STARTET
                    )
                )
            }
            response.status shouldBe HttpStatusCode.Forbidden
            val feil: Feil = response.body()
            feil shouldBe Feil(
                Under18Aar.beskrivelse, Feil.FeilKode.AVVIST, AarsakTilAvvisning(
                    beskrivelse = Under18Aar.beskrivelse,
                    regel = ApiRegelId.UNDER_18_AAR,
                    detaljer = listOf(Opplysning.ER_UNDER_18_AAR, Opplysning.BOSATT_ETTER_FREG_LOVEN)
                )
            )
        }
    }
})


