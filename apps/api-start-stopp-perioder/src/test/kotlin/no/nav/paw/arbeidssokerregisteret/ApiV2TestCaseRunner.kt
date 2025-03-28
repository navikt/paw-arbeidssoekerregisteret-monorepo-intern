package no.nav.paw.arbeidssokerregisteret

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.call.*
import io.ktor.server.auth.*
import io.ktor.server.testing.*
import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.FeilV2
import no.nav.paw.arbeidssokerregisteret.application.InngangsReglerV2
import no.nav.paw.arbeidssokerregisteret.auth.configureAuthentication
import no.nav.paw.arbeidssokerregisteret.plugins.configureHTTP
import no.nav.paw.arbeidssokerregisteret.plugins.configureSerialization
import no.nav.paw.arbeidssokerregisteret.routes.arbeidssokerRoutesV2
import no.nav.paw.arbeidssokerregisteret.testdata.StartPeriodeTestCase
import no.nav.paw.arbeidssokerregisteret.testdata.StoppPeriodeTestCase
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
    val startPeriodeTestCases = StartPeriodeTestCase::class.sealedSubclasses
    val stoppPeriodeTestCases = StoppPeriodeTestCase::class.sealedSubclasses
    val allTestCases = startPeriodeTestCases + stoppPeriodeTestCases
    val caseInstances: List<TestCase> = allTestCases.mapNotNull { it.objectInstance }
    "Verifiserer oppsett av test caser" - {
        "Det må finnes minst en  test" {
            allTestCases.shouldNotBeEmpty()
            caseInstances.shouldNotBeEmpty()
        }
        "Alle tester må ha 'objectInstance" - {
            allTestCases.forEach { case ->
                "${case.simpleName} må ha 'objectInstance'" {
                    case.objectInstance.shouldNotBeNull()
                    case.objectInstance.shouldBeInstanceOf<TestCase>()
                }
            }
            caseInstances.forEach { case ->
                case.shouldBeInstanceOf<TestCase>()
            }
        }
        println("Antall tester: ${caseInstances.size}")
    }
    "Test cases V2" - {
        caseInstances
            .forEach { testCase ->
                val logger = LoggerFactory.getLogger(testCase::class.java)
                "Test  API V2 ${testCase::class.simpleName?.readable()}" - {
                    "Verifiser API V2" - {
                        with(initTestCaseContext(InngangsReglerV2)) {
                            val testConfiguration = TestCaseBuilder(mockOAuthServer, autorisasjonService)
                                .also { testCase.configure(it) }
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
                                    val person = (testCase as? StartPeriodeTestCase)?.person
                                    logger.info("Running test for $id")
                                    personInfoService.setPersonInfo(id, person)
                                    val statusV2 =
                                        client.startStoppPeriode(
                                            periodeTilstand = testCase.tilstand,
                                            identitetsnummer = id,
                                            token = testConfiguration.authToken?.second,
                                            godkjent = (testCase as? StartPeriodeTestCase)?.forhaandsGodkjent ?: false,
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
                                        expected = expectedRecord,
                                        brukerAuth = testConfiguration.authToken?.first
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

