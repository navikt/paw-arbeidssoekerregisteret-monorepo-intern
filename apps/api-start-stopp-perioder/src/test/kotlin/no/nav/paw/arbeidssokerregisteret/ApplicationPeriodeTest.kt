package no.nav.paw.arbeidssokerregisteret

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
import no.nav.paw.arbeidssokerregisteret.routes.apiRegel
import no.nav.paw.arbeidssokerregisteret.routes.arbeidssokerRoutesV2
import no.nav.paw.arbeidssokerregisteret.routes.feilmeldingVedAvvist
import no.nav.paw.felles.collection.pawNonEmptyListOf

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
                    startStoppRequestHandler.startArbeidssokerperiode(any(), any(), any(), any())
                } returns muligGrunnlagForAvvisning(
                    regel = Regel(
                        id = Under18Aar,
                        kritierier = listOf(DomeneOpplysning.ErUnder18Aar),
                        vedTreff = ::skalAvises
                    ),
                    opplysninger = listOf(DomeneOpplysning.ErUnder18Aar, DomeneOpplysning.BosattEtterFregLoven)
                ).mapLeft { pawNonEmptyListOf(it) }
                arbeidssokerRoutesV2(startStoppRequestHandler)
            }
            val client = createClient {
                install(ContentNegotiation) {
                    jackson {
                        registerKotlinModule()
                        registerModule(JavaTimeModule())
                    }
                }
            }
            val response = client.put("/api/v2/arbeidssoker/periode") {
                headers {
                    append(HttpHeaders.ContentType, ContentType.Application.Json)
                }
                setBody(
                    ApiV2ArbeidssokerPeriodePutRequest(
                        identitetsnummer = "12345678911",
                        periodeTilstand = ApiV2ArbeidssokerPeriodePutRequest.PeriodeTilstand.STARTET
                    )
                )
            }
            response.status shouldBe HttpStatusCode.Forbidden
            val feil: FeilV2 = response.body()
            feil shouldBe FeilV2(
                melding = feilmeldingVedAvvist,
                feilKode = FeilV2.FeilKode.AVVIST,
                aarsakTilAvvisning = AarsakTilAvvisningV2(
                    regler = listOf(Under18Aar.apiRegel()),
                    detaljer = listOf(Opplysning.ER_UNDER_18_AAR, Opplysning.BOSATT_ETTER_FREG_LOVEN)
                )
            )
        }
    }
})
