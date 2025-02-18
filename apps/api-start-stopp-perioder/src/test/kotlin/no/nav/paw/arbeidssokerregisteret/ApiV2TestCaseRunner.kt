package no.nav.paw.arbeidssokerregisteret

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.ktor.client.call.*
import io.ktor.server.auth.*
import io.ktor.server.testing.*
import io.mockk.mockk
import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.FeilV2
import no.nav.paw.arbeidssokerregisteret.application.InngangsReglerV2
import no.nav.paw.arbeidssokerregisteret.auth.configureAuthentication
import no.nav.paw.arbeidssokerregisteret.plugins.configureHTTP
import no.nav.paw.arbeidssokerregisteret.plugins.configureSerialization
import no.nav.paw.arbeidssokerregisteret.routes.arbeidssokerRoutes
import no.nav.paw.arbeidssokerregisteret.routes.arbeidssokerRoutesV2
import no.nav.paw.arbeidssokerregisteret.testdata.TestCase
import no.nav.paw.arbeidssokerregisteret.testdata.TestCaseBuilder
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.slf4j.LoggerFactory


class ApiV2TestCaseRunner : FreeSpec({
    val mockOAuthServer = MockOAuth2Server()
    beforeSpec {
        mockOAuthServer.start()
    }
    afterSpec {
        mockOAuthServer.shutdown()
    }
    val testCases = TestCase::class.sealedSubclasses
    "Verifiserer oppsett av test caser" - {
        "Det må finnes minst en  test" {
            testCases.shouldNotBeEmpty()
        }
        "Alle tester må ha 'objectInstance" - {
            testCases.forEach { case ->
                "${case.simpleName} må ha 'objectInstance'" {
                    case.objectInstance.shouldNotBeNull()
                }
            }
        }
    }
    "Test cases V2" - {
        TestCase::class.sealedSubclasses
            .mapNotNull { it.objectInstance }
            .forEach { testCase ->
                val logger = LoggerFactory.getLogger(testCase::class.java)
                "Test  API V2 ${testCase::class.simpleName?.readable()}" - {
                    "Verifiser API V2" - {
                        with(initTestCaseContext(InngangsReglerV2)) {
                            "Verifiser API response" {
                                testApplication {
                                    application {
                                        configureSerialization()
                                        configureHTTP()
                                        configureAuthentication(mockOAuthServer)
                                    }
                                    routing {
                                        authenticate("tokenx", "azure") {
                                            arbeidssokerRoutesV2(startStoppRequestHandler)
                                        }
                                    }
                                    val client = createClient { defaultConfig() }
                                    val id = testCase.id
                                    val person = testCase.person
                                    logger.info("Running test for $id")
                                    personInfoService.setPersonInfo(id, person)
                                    val testConfiguration = TestCaseBuilder(mockOAuthServer, autorisasjonService)
                                        .also { testCase.configure(it) }
                                    val statusV2 =
                                        client.startPeriodeV2(
                                            id,
                                            testConfiguration.authToken,
                                            testCase.forhaandsGodkjent,
                                            feilretting = testCase.feilretting
                                        )
                                    statusV2.status shouldBe testCase.producesHttpResponse
                                    testCase.producesError?.also { expectedErrorResponse ->
                                        val body = statusV2.body<FeilV2>()
                                        body.feilKode.name shouldBe expectedErrorResponse.feilKode.name
                                        val forventetMelding: String =
                                            if (expectedErrorResponse.feilKode == FeilV2.FeilKode.IKKE_TILGANG) expectedErrorResponse.melding else "Avvist, se 'aarsakTilAvvisning' for detaljer"
                                        body.melding shouldBe forventetMelding
                                        expectedErrorResponse.aarsakTilAvvisning should { aarsak ->
                                            val fakitskAarsak = body.aarsakTilAvvisning
                                            if (aarsak == null) {
                                                fakitskAarsak.shouldBeNull()
                                            } else {
                                                fakitskAarsak?.detaljer shouldContainExactlyInAnyOrder aarsak.detaljer
                                                fakitskAarsak?.regler shouldContainExactlyInAnyOrder aarsak.regler
                                            }
                                        }
                                        expectedErrorResponse.aarsakTilAvvisning?.detaljer?.also { expectedDetails ->
                                            body.aarsakTilAvvisning?.detaljer.shouldNotBeNull()
                                            body.aarsakTilAvvisning?.detaljer?.shouldContainExactlyInAnyOrder(
                                                expectedDetails
                                            )
                                        }
                                    }
                                }
                            }
                            "Verifiser Kafka melding" {
                                val expectedRecord = testCase.producesRecord(kafkaKeys)
                                if (expectedRecord != null) {
                                    verify(
                                        actual = producer.next(),
                                        expected = expectedRecord
                                    )
                                    producer.next().shouldBeNull()
                                } else {
                                    producer.next().shouldBeNull()
                                }
                            }
                        }
                    }
                }
            }
    }
})

