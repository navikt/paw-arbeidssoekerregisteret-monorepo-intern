package no.nav.paw.arbeidssoekerregisteret.backup

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssoekerregisteret.backup.health.isDatabaseReady

class DatabaseReadyTest : FreeSpec({
    with(TestApplicationContext.buildWithDatabase()) {
        "Database readiness check returnerer true når db er tilgjengelig" {
            isDatabaseReady(dataSource) shouldBe true
        }
        "Database readiness check returnerer false når db ikke er tilgjengelig" {
            dataSource.close()
            isDatabaseReady(dataSource) shouldBe false
        }
    }
})