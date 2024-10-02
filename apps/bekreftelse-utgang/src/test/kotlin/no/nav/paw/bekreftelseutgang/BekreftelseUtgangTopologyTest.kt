package no.nav.paw.bekreftelseutgang

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssoekerregisteret.testdata.kafkaKeyContext
import java.time.Instant


class BekreftelseUtgangTopologyTest : FreeSpec({

    val startTime = Instant.ofEpochMilli(1704185347000)

    "test" {
        with(ApplicationTestContext(initialWallClockTime = startTime)) {
            true shouldBe true
        }
    }

    "test 2" {
        with(ApplicationTestContext(initialWallClockTime = startTime)) {
            with(kafkaKeyContext()) {
                true shouldBe true
            }
        }
    }

})
