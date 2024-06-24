package no.nav.paw.arbeidssokerregisteret

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.client.call.*
import io.ktor.server.auth.*
import io.ktor.server.testing.*
import io.mockk.mockk
import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.Feil
import no.nav.paw.arbeidssokerregisteret.application.RequestValidator
import no.nav.paw.arbeidssokerregisteret.application.StartStoppRequestHandler
import no.nav.paw.arbeidssokerregisteret.auth.configureAuthentication
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.plugins.configureHTTP
import no.nav.paw.arbeidssokerregisteret.plugins.configureSerialization
import no.nav.paw.arbeidssokerregisteret.routes.arbeidssokerRoutes
import no.nav.paw.arbeidssokerregisteret.services.AutorisasjonService
import no.nav.paw.arbeidssokerregisteret.services.PersonInfoService
import no.nav.paw.arbeidssokerregisteret.testdata.TestCase
import no.nav.paw.arbeidssokerregisteret.testdata.TestCaseBuilder
import no.nav.paw.kafkakeygenerator.client.inMemoryKafkaKeysMock
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.slf4j.LoggerFactory

class TestCaseRunner : FreeSpec({
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
    "Test cases" - {
        TestCase::class.sealedSubclasses
            .mapNotNull { it.objectInstance }
            .forEach { testCase ->
                val logger = LoggerFactory.getLogger(testCase::class.java)
                "Test ${testCase::class.simpleName?.readable()}" {
                    testApplication {
                        application {
                            configureSerialization()
                            configureHTTP()
                            configureAuthentication(mockOAuthServer)
                        }
                        val autorisasjonService = mockk<AutorisasjonService>()
                        val personInfoService = mockk<PersonInfoService>()
                        val producer: ProducerMock<Long, Hendelse> = ProducerMock()
                        val kafkaKeys = inMemoryKafkaKeysMock()
                        val startStoppRequestHandler = StartStoppRequestHandler(
                            hendelseTopic = "any",
                            requestValidator = RequestValidator(
                                autorisasjonService = autorisasjonService,
                                personInfoService = personInfoService
                            ),
                            producer = producer,
                            kafkaKeysClient = kafkaKeys
                        )
                        routing {
                            authenticate("tokenx", "azure") {
                                arbeidssokerRoutes(startStoppRequestHandler, mockk())
                            }
                        }
                        val client = createClient { defaultConfig() }
                        val id = testCase.id
                        val person = testCase.person
                        logger.info("Running test for $id")
                        personInfoService.setPersonInfo(id, person)
                        val testConfiguration = TestCaseBuilder(mockOAuthServer, autorisasjonService)
                            .also { testCase.configure(it) }
                        val status = client.startPeriode(id, testConfiguration.authToken, testCase.forhaandsGodkjent)
                        status.status shouldBe testCase.producesHttpResponse
                        testCase.producesError?.also { expectedErrorResponse ->
                            val body = status.body<Feil>()
                            body.feilKode shouldBe expectedErrorResponse.feilKode
                            body.aarsakTilAvvisning?.regel shouldBe expectedErrorResponse.aarsakTilAvvisning?.regel
                            expectedErrorResponse.aarsakTilAvvisning?.detaljer?.also { expectedDetails ->
                                body.aarsakTilAvvisning?.detaljer.shouldNotBeNull()
                                body.aarsakTilAvvisning?.detaljer?.shouldContainExactlyInAnyOrder(expectedDetails)
                            }
                        }
                        val expectedRecord = testCase.producesRecord(kafkaKeys)
                        if (expectedRecord != null) {
                            verify(
                                actual = producer.next(),
                                expected = expectedRecord
                            )
                        } else {
                            producer.next().shouldBeNull()
                        }
                    }
                }
            }
    }
})

fun String.readable(): String =
    map { letter -> if (letter.isUpperCase()) " ${letter.lowercase()}" else "$letter" }
        .joinToString("")
        .replace("oe", "ø")
        .replace("aa", "å")
