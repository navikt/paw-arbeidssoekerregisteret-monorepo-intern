package no.nav.paw.arbeidssoeker.synk.service

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode
import no.nav.paw.arbeidssoeker.synk.context.TestContext
import no.nav.paw.arbeidssoeker.synk.test.ErrorResponse

class ArbeidssoekerSynkServiceTest : FreeSpec({
    with(TestContext()) {

        beforeSpec {
            initDatabase()
        }

        "Skal lese CSV-fil" {
            var responseMapping = mapOf(
                "03017012345" to ErrorResponse.ikkeTilgang,
                "06017012345" to ErrorResponse.avvist,
                "08017012345" to ErrorResponse.ukjentFeil
            )


            initArbeidssoekerSynkService(responseMapping)
                .synkArbeidssoekere(filePath)
            var rows = arbeidssoekerSynkRepository.find()

            rows shouldHaveSize 10
            rows[0].version shouldBe "v1.csv"
            rows[0].identitetsnummer shouldBe "01017012345"
            rows[0].status shouldBe HttpStatusCode.NoContent.value
            rows[1].version shouldBe "v1.csv"
            rows[1].identitetsnummer shouldBe "02017012345"
            rows[1].status shouldBe HttpStatusCode.NoContent.value
            rows[2].version shouldBe "v1.csv"
            rows[2].identitetsnummer shouldBe "03017012345"
            rows[2].status shouldBe HttpStatusCode.Forbidden.value
            rows[3].version shouldBe "v1.csv"
            rows[3].identitetsnummer shouldBe "04017012345"
            rows[3].status shouldBe HttpStatusCode.NoContent.value
            rows[4].version shouldBe "v1.csv"
            rows[4].identitetsnummer shouldBe "05017012345"
            rows[4].status shouldBe HttpStatusCode.NoContent.value
            rows[5].version shouldBe "v1.csv"
            rows[5].identitetsnummer shouldBe "06017012345"
            rows[5].status shouldBe HttpStatusCode.BadRequest.value
            rows[6].version shouldBe "v1.csv"
            rows[6].identitetsnummer shouldBe "07017012345"
            rows[6].status shouldBe HttpStatusCode.NoContent.value
            rows[7].version shouldBe "v1.csv"
            rows[7].identitetsnummer shouldBe "08017012345"
            rows[7].status shouldBe HttpStatusCode.InternalServerError.value
            rows[8].version shouldBe "v1.csv"
            rows[8].identitetsnummer shouldBe "09017012345"
            rows[8].status shouldBe HttpStatusCode.NoContent.value
            rows[9].version shouldBe "v1.csv"
            rows[9].identitetsnummer shouldBe "10017012345"
            rows[9].status shouldBe HttpStatusCode.NoContent.value

            responseMapping = mapOf(
                "01017012345" to ErrorResponse.ikkeTilgang,
                "02017012345" to ErrorResponse.avvist,
                "08017012345" to ErrorResponse.ukjentFeil
            )

            initArbeidssoekerSynkService(responseMapping)
                .synkArbeidssoekere(filePath)
            rows = arbeidssoekerSynkRepository.find()

            rows shouldHaveSize 10
            rows[0].version shouldBe "v1.csv"
            rows[0].identitetsnummer shouldBe "01017012345"
            rows[0].status shouldBe HttpStatusCode.NoContent.value
            rows[1].version shouldBe "v1.csv"
            rows[1].identitetsnummer shouldBe "02017012345"
            rows[1].status shouldBe HttpStatusCode.NoContent.value
            rows[2].version shouldBe "v1.csv"
            rows[2].identitetsnummer shouldBe "03017012345"
            rows[2].status shouldBe HttpStatusCode.NoContent.value
            rows[3].version shouldBe "v1.csv"
            rows[3].identitetsnummer shouldBe "04017012345"
            rows[3].status shouldBe HttpStatusCode.NoContent.value
            rows[4].version shouldBe "v1.csv"
            rows[4].identitetsnummer shouldBe "05017012345"
            rows[4].status shouldBe HttpStatusCode.NoContent.value
            rows[5].version shouldBe "v1.csv"
            rows[5].identitetsnummer shouldBe "06017012345"
            rows[5].status shouldBe HttpStatusCode.NoContent.value
            rows[6].version shouldBe "v1.csv"
            rows[6].identitetsnummer shouldBe "07017012345"
            rows[6].status shouldBe HttpStatusCode.NoContent.value
            rows[7].version shouldBe "v1.csv"
            rows[7].identitetsnummer shouldBe "08017012345"
            rows[7].status shouldBe HttpStatusCode.InternalServerError.value
            rows[8].version shouldBe "v1.csv"
            rows[8].identitetsnummer shouldBe "09017012345"
            rows[8].status shouldBe HttpStatusCode.NoContent.value
            rows[9].version shouldBe "v1.csv"
            rows[9].identitetsnummer shouldBe "10017012345"
            rows[9].status shouldBe HttpStatusCode.NoContent.value
        }
    }
})