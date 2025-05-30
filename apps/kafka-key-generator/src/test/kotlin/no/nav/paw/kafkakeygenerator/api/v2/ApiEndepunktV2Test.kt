package no.nav.paw.kafkakeygenerator.api.v2

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import no.nav.paw.identitet.internehendelser.vo.IdentitetType
import no.nav.paw.kafkakeygenerator.context.TestContext
import no.nav.paw.kafkakeygenerator.context.TestContext.Companion.setJsonBody
import no.nav.paw.kafkakeygenerator.model.IdentitetStatus
import no.nav.paw.kafkakeygenerator.test.TestData
import no.nav.paw.kafkakeygenerator.vo.ArbeidssoekerId
import no.nav.paw.kafkakeygenerator.vo.Identitetsnummer

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

                val response1 = client.post("/api/v2/hentEllerOpprett") {
                    bearerAuth(token.serialize())
                    setJsonBody(RequestV2(ident = TestData.dnr4))
                }
                response1.status shouldBe HttpStatusCode.OK
                val body1 = response1.body<ResponseV2>()
                body1.id shouldBe 1
                body1.key shouldBe publicTopicKeyFunction(ArbeidssoekerId(body1.id)).value

                val kafkaKeyRow1 = kafkaKeysIdentitetRepository.find(Identitetsnummer(TestData.dnr4))
                kafkaKeyRow1 shouldNotBe null
                kafkaKeyRow1!!.identitetsnummer shouldBe TestData.dnr4
                kafkaKeyRow1.arbeidssoekerId shouldBe body1.id

                val identitetRow1 = identitetRepository.getByIdentitet(TestData.dnr4)
                identitetRow1 shouldNotBe null
                identitetRow1!!.aktorId shouldBe TestData.aktorId4
                identitetRow1.identitet shouldBe TestData.dnr4
                val identitetRows1 = identitetRepository.findByAktorId(identitetRow1.aktorId)
                identitetRows1.size shouldBe 3
                identitetRows1[0].aktorId shouldBe TestData.aktorId4
                identitetRows1[0].arbeidssoekerId shouldBe body1.id
                identitetRows1[0].identitet shouldBe TestData.dnr4
                identitetRows1[0].type shouldBe IdentitetType.FOLKEREGISTERIDENT
                identitetRows1[0].gjeldende shouldBe true
                identitetRows1[0].status shouldBe IdentitetStatus.AKTIV
                identitetRows1[1].aktorId shouldBe TestData.aktorId4
                identitetRows1[1].arbeidssoekerId shouldBe body1.id
                identitetRows1[1].identitet shouldBe TestData.aktorId4
                identitetRows1[1].type shouldBe IdentitetType.AKTORID
                identitetRows1[1].gjeldende shouldBe true
                identitetRows1[1].status shouldBe IdentitetStatus.AKTIV
                identitetRows1[2].aktorId shouldBe TestData.aktorId4
                identitetRows1[2].arbeidssoekerId shouldBe body1.id
                identitetRows1[2].identitet shouldBe TestData.npId4
                identitetRows1[2].type shouldBe IdentitetType.NPID
                identitetRows1[2].gjeldende shouldBe true
                identitetRows1[2].status shouldBe IdentitetStatus.AKTIV

                val response2 = client.post("/api/v2/hentEllerOpprett") {
                    bearerAuth(token.serialize())
                    setJsonBody(RequestV2(ident = TestData.fnr4_1))
                }
                response2.status shouldBe HttpStatusCode.OK
                val body2 = response2.body<ResponseV2>()
                body2.id shouldBe body1.id
                body2.key shouldBe body1.key

                val kafkaKeyRow2 = kafkaKeysIdentitetRepository.find(Identitetsnummer(TestData.fnr4_1))
                kafkaKeyRow2 shouldNotBe null
                kafkaKeyRow2!!.identitetsnummer shouldBe TestData.fnr4_1
                kafkaKeyRow2.arbeidssoekerId shouldBe body2.id

                val identitetRow2 = identitetRepository.getByIdentitet(TestData.fnr4_1)
                identitetRow2 shouldNotBe null
                identitetRow2!!.aktorId shouldBe TestData.aktorId4
                identitetRow2.identitet shouldBe TestData.fnr4_1
                val identitetRows2 = identitetRepository.findByAktorId(identitetRow2.aktorId)
                identitetRows2.size shouldBe 4
                identitetRows2[0].aktorId shouldBe TestData.aktorId4
                identitetRows2[0].arbeidssoekerId shouldBe body2.id
                identitetRows2[0].identitet shouldBe TestData.dnr4
                identitetRows2[0].type shouldBe IdentitetType.FOLKEREGISTERIDENT
                identitetRows2[0].gjeldende shouldBe false
                identitetRows2[0].status shouldBe IdentitetStatus.AKTIV
                identitetRows2[1].aktorId shouldBe TestData.aktorId4
                identitetRows2[1].arbeidssoekerId shouldBe body2.id
                identitetRows2[1].identitet shouldBe TestData.aktorId4
                identitetRows2[1].type shouldBe IdentitetType.AKTORID
                identitetRows2[1].gjeldende shouldBe true
                identitetRows2[1].status shouldBe IdentitetStatus.AKTIV
                identitetRows2[2].aktorId shouldBe TestData.aktorId4
                identitetRows2[2].arbeidssoekerId shouldBe body2.id
                identitetRows2[2].identitet shouldBe TestData.npId4
                identitetRows2[2].type shouldBe IdentitetType.NPID
                identitetRows2[2].gjeldende shouldBe true
                identitetRows2[2].status shouldBe IdentitetStatus.AKTIV
                identitetRows2[3].aktorId shouldBe TestData.aktorId4
                identitetRows2[3].arbeidssoekerId shouldBe body2.id
                identitetRows2[3].identitet shouldBe TestData.fnr4_1
                identitetRows2[3].type shouldBe IdentitetType.FOLKEREGISTERIDENT
                identitetRows2[3].gjeldende shouldBe true
                identitetRows2[3].status shouldBe IdentitetStatus.AKTIV
            }
        }
    }
})