package no.nav.paw.arbeidssoeker.synk.service

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode
import no.nav.paw.arbeidssoeker.synk.context.TestContext
import no.nav.paw.arbeidssoeker.synk.test.ErrorResponse
import kotlin.io.path.name

class ArbeidssoekerSynkServiceTest : FreeSpec({
    with(TestContext()) {

        beforeSpec {
            initDatabase()
        }

        "Skal lese CSV-fil" {
            setMockHttpClientResponses(
                mapOf(
                    "03017012345" to ErrorResponse.ikkeTilgang,
                    "06017012345" to ErrorResponse.avvist,
                    "08017012345" to ErrorResponse.ukjentFeil
                )
            )

            val version = filePath.name
            var fileRows = csvReader.readValues(filePath)
            arbeidssoekerSynkService.synkArbeidssoekere(version, fileRows)
            var databaseRows = arbeidssoekerSynkRepository.find()

            databaseRows shouldHaveSize 10
            databaseRows[0].version shouldBe version
            databaseRows[0].identitetsnummer shouldBe "01017012345"
            databaseRows[0].status shouldBe HttpStatusCode.NoContent.value
            databaseRows[1].version shouldBe version
            databaseRows[1].identitetsnummer shouldBe "02017012345"
            databaseRows[1].status shouldBe HttpStatusCode.NoContent.value
            databaseRows[2].version shouldBe version
            databaseRows[2].identitetsnummer shouldBe "03017012345"
            databaseRows[2].status shouldBe HttpStatusCode.Forbidden.value
            databaseRows[3].version shouldBe version
            databaseRows[3].identitetsnummer shouldBe "04017012345"
            databaseRows[3].status shouldBe HttpStatusCode.NoContent.value
            databaseRows[4].version shouldBe version
            databaseRows[4].identitetsnummer shouldBe "05017012345"
            databaseRows[4].status shouldBe HttpStatusCode.NoContent.value
            databaseRows[5].version shouldBe version
            databaseRows[5].identitetsnummer shouldBe "06017012345"
            databaseRows[5].status shouldBe HttpStatusCode.BadRequest.value
            databaseRows[6].version shouldBe version
            databaseRows[6].identitetsnummer shouldBe "07017012345"
            databaseRows[6].status shouldBe HttpStatusCode.NoContent.value
            databaseRows[7].version shouldBe version
            databaseRows[7].identitetsnummer shouldBe "08017012345"
            databaseRows[7].status shouldBe HttpStatusCode.InternalServerError.value
            databaseRows[8].version shouldBe version
            databaseRows[8].identitetsnummer shouldBe "09017012345"
            databaseRows[8].status shouldBe HttpStatusCode.NoContent.value
            databaseRows[9].version shouldBe version
            databaseRows[9].identitetsnummer shouldBe "10017012345"
            databaseRows[9].status shouldBe HttpStatusCode.NoContent.value

            setMockHttpClientResponses(
                mapOf(
                    "01017012345" to ErrorResponse.ikkeTilgang,
                    "02017012345" to ErrorResponse.avvist,
                    "08017012345" to ErrorResponse.ukjentFeil
                )
            )

            fileRows = csvReader.readValues(filePath)
            arbeidssoekerSynkService.synkArbeidssoekere(filePath.name, fileRows)
            databaseRows = arbeidssoekerSynkRepository.find()

            databaseRows shouldHaveSize 10
            databaseRows[0].version shouldBe version
            databaseRows[0].identitetsnummer shouldBe "01017012345"
            databaseRows[0].status shouldBe HttpStatusCode.NoContent.value
            databaseRows[1].version shouldBe version
            databaseRows[1].identitetsnummer shouldBe "02017012345"
            databaseRows[1].status shouldBe HttpStatusCode.NoContent.value
            databaseRows[2].version shouldBe version
            databaseRows[2].identitetsnummer shouldBe "03017012345"
            databaseRows[2].status shouldBe HttpStatusCode.NoContent.value
            databaseRows[3].version shouldBe version
            databaseRows[3].identitetsnummer shouldBe "04017012345"
            databaseRows[3].status shouldBe HttpStatusCode.NoContent.value
            databaseRows[4].version shouldBe version
            databaseRows[4].identitetsnummer shouldBe "05017012345"
            databaseRows[4].status shouldBe HttpStatusCode.NoContent.value
            databaseRows[5].version shouldBe version
            databaseRows[5].identitetsnummer shouldBe "06017012345"
            databaseRows[5].status shouldBe HttpStatusCode.NoContent.value
            databaseRows[6].version shouldBe version
            databaseRows[6].identitetsnummer shouldBe "07017012345"
            databaseRows[6].status shouldBe HttpStatusCode.NoContent.value
            databaseRows[7].version shouldBe version
            databaseRows[7].identitetsnummer shouldBe "08017012345"
            databaseRows[7].status shouldBe HttpStatusCode.InternalServerError.value
            databaseRows[8].version shouldBe version
            databaseRows[8].identitetsnummer shouldBe "09017012345"
            databaseRows[8].status shouldBe HttpStatusCode.NoContent.value
            databaseRows[9].version shouldBe version
            databaseRows[9].identitetsnummer shouldBe "10017012345"
            databaseRows[9].status shouldBe HttpStatusCode.NoContent.value
        }
    }
})