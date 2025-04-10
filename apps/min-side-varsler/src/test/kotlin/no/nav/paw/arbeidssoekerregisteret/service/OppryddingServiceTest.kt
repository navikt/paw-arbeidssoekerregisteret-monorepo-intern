package no.nav.paw.arbeidssoekerregisteret.service

import io.kotest.core.spec.style.FreeSpec
import no.nav.paw.arbeidssoekerregisteret.context.TestContext
import no.nav.paw.arbeidssoekerregisteret.test.TestData
import java.time.Duration
import java.time.Instant

class OppryddingServiceTest : FreeSpec({
    with(TestContext.buildWithH2()) {
        with(TestData) {
            beforeTest { initDatabase() }
            val bestiller = "TEST_BRUKER"

            "Skal rydde i databasen" {
                varselRow(updatedTimestamp = Instant.now().minus(Duration.ofDays(1)))
            }
        }
    }
})