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
import io.mockk.every
import io.mockk.mockk
import no.nav.paw.arbeidssokerregisteret.application.*
import no.nav.paw.arbeidssokerregisteret.domain.Feilkode
import no.nav.paw.arbeidssokerregisteret.domain.http.AarsakTilAvvisning
import no.nav.paw.arbeidssokerregisteret.domain.http.Feil
import no.nav.paw.arbeidssokerregisteret.domain.http.PeriodeTilstand
import no.nav.paw.arbeidssokerregisteret.domain.http.StartStoppRequest
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.plugins.configureHTTP
import no.nav.paw.arbeidssokerregisteret.plugins.configureSerialization
import no.nav.paw.arbeidssokerregisteret.routes.arbeidssokerRoutes
import no.nav.paw.arbeidssokerregisteret.services.AutorisasjonService
import no.nav.paw.arbeidssokerregisteret.services.PersonInfoService
import no.nav.paw.migrering.app.kafkakeys.inMemoryKafkaKeysMock
import no.nav.paw.pdl.PdlClient
import no.nav.paw.pdl.hentPerson
import org.apache.kafka.clients.producer.Producer

class ApplicationTest : FunSpec({
    test("Verifiser at vi returnerer 'Feil' objekt når vi avviser en periode") {
        testApplication {
            application {
                configureSerialization()
                configureHTTP()
            }
            routing {
                val requestHandler = mockk<RequestHandler>()
                coEvery {
                    with(any<RequestScope>()) {
                        requestHandler.startArbeidssokerperiode(any())
                    }
                } returns Avvist(
                    regel = Regel(
                        kode = 1, beskrivelse = "under 18 år",
                        opplysninger = listOf(Opplysning.ER_UNDER_18_AAR),
                        vedTreff = ::Avvist
                    ),
                    opplysning = listOf(Opplysning.ER_UNDER_18_AAR)
                )
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
            val response = client.put("/api/v1/arbeidssoker/periode") {
                headers {
                    append(HttpHeaders.ContentType, ContentType.Application.Json)
                }
                setBody(
                    StartStoppRequest(
                        identitetsnummer = "12345678911",
                        periodeTilstand = PeriodeTilstand.STARTET
                    )
                )
            }
            response.status shouldBe HttpStatusCode.Forbidden
            val feil: Feil = response.body()
            feil shouldBe Feil(
                "under 18 år", Feilkode.AVVIST, AarsakTilAvvisning(
                    beskrivelse = "under 18 år",
                    kode = 1,
                    detaljer = setOf(Opplysning.ER_UNDER_18_AAR)
                )
            )
        }
    }
})


