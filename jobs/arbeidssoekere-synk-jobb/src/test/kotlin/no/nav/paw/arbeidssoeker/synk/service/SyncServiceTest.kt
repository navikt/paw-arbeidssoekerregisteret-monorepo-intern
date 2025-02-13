package no.nav.paw.arbeidssoeker.synk.service

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssoeker.synk.repository.ArbeidssoekerSynkRepository
import no.nav.paw.arbeidssoeker.synk.test.createTestDataSource
import no.nav.paw.arbeidssoeker.synk.test.flywayMigrate
import org.jetbrains.exposed.sql.Database
import kotlin.io.path.toPath

class SyncServiceTest : FreeSpec({

    beforeSpec {
        val dataSource = createTestDataSource()
            .flywayMigrate()
        Database.connect(dataSource)
    }

    "Skal lese CSV-fil" {
        val url = javaClass.getResource("/v1.csv")!!
        val path = url.toURI().toPath()
        val arbeidssoekerSynkRepository = ArbeidssoekerSynkRepository()
        val arbeidssoekerSynkService = ArbeidssoekerSynkService(arbeidssoekerSynkRepository)
        arbeidssoekerSynkService.synkArbeidssoekere(path)
        val rows = arbeidssoekerSynkRepository.find()

        rows shouldHaveSize 5
        rows[0].version shouldBe "v1.csv"
        rows[0].identitetsnummer shouldBe "01017012345"
        rows[0].status shouldBe 200
        rows[1].version shouldBe "v1.csv"
        rows[1].identitetsnummer shouldBe "02017012345"
        rows[1].status shouldBe 200
        rows[2].version shouldBe "v1.csv"
        rows[2].identitetsnummer shouldBe "03017012345"
        rows[2].status shouldBe 200
        rows[3].version shouldBe "v1.csv"
        rows[3].identitetsnummer shouldBe "04017012345"
        rows[3].status shouldBe 200
        rows[4].version shouldBe "v1.csv"
        rows[4].identitetsnummer shouldBe "05017012345"
        rows[4].status shouldBe 200
    }
})