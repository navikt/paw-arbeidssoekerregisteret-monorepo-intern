package no.nav.paw.arbeidssokerregisteret

import arrow.core.right
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import no.nav.paw.arbeidssoekerregisteret.api.opplysningermottatt.models.*
import no.nav.paw.arbeidssoekerregisteret.api.opplysningermottatt.models.JobbsituasjonMedDetaljer.Beskrivelse.DELTIDSJOBB_VIL_MER
import no.nav.paw.arbeidssoekerregisteret.api.opplysningermottatt.models.JobbsituasjonMedDetaljer.Beskrivelse.HAR_SAGT_OPP
import no.nav.paw.arbeidssokerregisteret.application.OpplysningerRequestHandler
import no.nav.paw.arbeidssokerregisteret.application.StartStoppRequestHandler
import no.nav.paw.arbeidssokerregisteret.plugins.configureHTTP
import no.nav.paw.arbeidssokerregisteret.plugins.configureSerialization
import no.nav.paw.arbeidssokerregisteret.routes.arbeidssokerRoutes
import java.time.LocalDate

class ApplicationOpplysningerTest : FunSpec({
    test("Verifiser opplysninger happy path") {
        testApplication {
            application {
                configureSerialization()
                configureHTTP()
            }
            val opplysningerRequestHandler = mockk<OpplysningerRequestHandler>()
            routing {
                coEvery {
                    opplysningerRequestHandler.opprettBrukeropplysninger(any(), any())
                } returns Unit.right()
                arbeidssokerRoutes(opplysningerRequestHandler)
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
                            "nus":"1"},
                            "helse":{
                                "helsetilstandHindrerArbeid":"JA"
                            },
                            "jobbsituasjon":{
                                "beskrivelser":[
                                    {
                                        "beskrivelse":"$HAR_SAGT_OPP",
                                        "detaljer":{
                                            "$STILLING":"Bilskadereparatør",
                                            "$STILLING_STYRK08":"7213",
                                            "$GJELDER_FRA_DATO":"2021-04-29",
                                            "$GJELDER_TIL_DATO":"2021-05-29",
                                            "$SISTE_DAG_MED_LOENN":"2021-06-29",
                                            "$SISTE_ARBEIDSDAG":"2021-07-29",
                                            "$PROSENT": "75"
                                        }
                                    },
                                    {
                                        "beskrivelse":"$DELTIDSJOBB_VIL_MER"
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
            coVerify {
                val expectedRequest = OpplysningerRequest(
                    identitetsnummer = "12345678900",
                    opplysningerOmArbeidssoeker = OpplysningerOmArbeidssoeker(
                        utdanning = Utdanning(nus = "1"),
                        helse = Helse(helsetilstandHindrerArbeid = JaNeiVetIkke.JA),
                        jobbsituasjon = Jobbsituasjon(
                            listOf(
                                JobbsituasjonMedDetaljer(
                                    beskrivelse = HAR_SAGT_OPP,
                                    detaljer = Detaljer(
                                        stilling = "Bilskadereparatør",
                                        stillingStyrk08 = "7213",
                                        gjelderFraDatoIso8601 = LocalDate.parse("2021-04-29"),
                                        gjelderTilDatoIso8601 = LocalDate.parse("2021-05-29"),
                                        sisteDagMedLoennIso8601 = LocalDate.parse("2021-06-29"),
                                        sisteArbeidsdagIso8601 = LocalDate.parse("2021-07-29"),
                                        prosent = "75"
                                    )
                                ),
                                JobbsituasjonMedDetaljer(beskrivelse = DELTIDSJOBB_VIL_MER)
                            )
                        ),
                        annet = Annet(andreForholdHindrerArbeid = JaNeiVetIkke.NEI)
                    )
                )
                opplysningerRequestHandler.opprettBrukeropplysninger(any(), expectedRequest)
            }
        }
    }
})
