package no.nav.paw.kafkakeygenerator.api.v2

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import no.nav.paw.identitet.internehendelser.vo.Identitet
import no.nav.paw.identitet.internehendelser.vo.IdentitetType
import no.nav.paw.kafkakeygenerator.context.TestContext
import no.nav.paw.kafkakeygenerator.context.TestContext.Companion.setJsonBody
import no.nav.paw.kafkakeygenerator.model.IdentitetStatus
import no.nav.paw.kafkakeygenerator.test.IdentitetWrapper
import no.nav.paw.kafkakeygenerator.test.TestData
import no.nav.paw.kafkakeygenerator.test.TestData.asIdentitetsnummer
import no.nav.paw.kafkakeygenerator.test.asWrapper
import no.nav.paw.kafkakeygenerator.vo.ArbeidssoekerId

class ApiEndepunktV2Test : FreeSpec({
    with(TestContext.buildWithPostgres()) {

        beforeSpec {
            setUp()
            mockOAuth2Server.start()
        }

        afterSpec {
            tearDown()
            mockOAuth2Server.shutdown()
        }

        "Skal opprette identiteter for person" {
            testApplication {
                configureWebApplication()
                val client = buildTestClient()
                val token = mockOAuth2Server.issueAzureToken()

                // GIVEN
                val aktorId = Identitet(TestData.aktorId4, IdentitetType.AKTORID, true)
                val npId = Identitet(TestData.npId4, IdentitetType.NPID, true)
                val dnr = Identitet(TestData.dnr4, IdentitetType.FOLKEREGISTERIDENT, true)
                val fnr = Identitet(TestData.fnr4_1, IdentitetType.FOLKEREGISTERIDENT, true)

                // WHEN
                val response1 = client.post("/api/v2/hentEllerOpprett") {
                    bearerAuth(token.serialize())
                    setJsonBody(RequestV2(ident = dnr.identitet))
                }

                // THEN
                response1.status shouldBe HttpStatusCode.OK
                val body1 = response1.body<ResponseV2>()
                body1.id shouldBe 1
                body1.key shouldBe publicTopicKeyFunction(ArbeidssoekerId(body1.id)).value

                val kafkaKeyRow1 = kafkaKeysIdentitetRepository.find(dnr.asIdentitetsnummer())
                kafkaKeyRow1 shouldNotBe null
                kafkaKeyRow1!!.identitetsnummer shouldBe dnr.identitet
                kafkaKeyRow1.arbeidssoekerId shouldBe body1.id

                /* TODO Utkoblet
                val identitetRow1 = identitetRepository.getByIdentitet(dnr.identitet)
                identitetRow1 shouldNotBe null
                identitetRow1!!.aktorId shouldBe aktorId.identitet
                identitetRow1.identitet shouldBe dnr.identitet
                val identitetRows1 = identitetRepository.findByAktorId(identitetRow1.aktorId)
                identitetRows1.size shouldBe 3
                identitetRows1.map { it.asWrapper() } shouldContainOnly listOf(
                    IdentitetWrapper(
                        arbeidssoekerId = body1.id,
                        aktorId = aktorId.identitet,
                        identitet = aktorId,
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = body1.id,
                        aktorId = aktorId.identitet,
                        identitet = npId,
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = body1.id,
                        aktorId = aktorId.identitet,
                        identitet = dnr,
                        status = IdentitetStatus.AKTIV
                    )
                )*/

                // WHEN
                val response2 = client.post("/api/v2/hentEllerOpprett") {
                    bearerAuth(token.serialize())
                    setJsonBody(RequestV2(ident = fnr.identitet))
                }

                // THEN
                response2.status shouldBe HttpStatusCode.OK
                val body2 = response2.body<ResponseV2>()
                body2.id shouldBe body1.id
                body2.key shouldBe body1.key

                val kafkaKeyRow2 = kafkaKeysIdentitetRepository.find(fnr.asIdentitetsnummer())
                kafkaKeyRow2 shouldNotBe null
                kafkaKeyRow2!!.identitetsnummer shouldBe fnr.identitet
                kafkaKeyRow2.arbeidssoekerId shouldBe body2.id

                /* TODO: Utkoblet
                val identitetRow2 = identitetRepository.getByIdentitet(fnr.identitet)
                identitetRow2 shouldNotBe null
                identitetRow2!!.aktorId shouldBe aktorId.identitet
                identitetRow2.identitet shouldBe fnr.identitet
                val identitetRows2 = identitetRepository.findByAktorId(identitetRow2.aktorId)
                identitetRows2.size shouldBe 4
                identitetRows2.map { it.asWrapper() } shouldContainOnly listOf(
                    IdentitetWrapper(
                        arbeidssoekerId = body1.id,
                        aktorId = aktorId.identitet,
                        identitet = aktorId,
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = body1.id,
                        aktorId = aktorId.identitet,
                        identitet = npId,
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = body1.id,
                        aktorId = aktorId.identitet,
                        identitet = dnr.copy(gjeldende = false),
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = body1.id,
                        aktorId = aktorId.identitet,
                        identitet = fnr,
                        status = IdentitetStatus.AKTIV
                    )
                )*/
            }
        }
    }
})