package no.nav.paw.bekreftelsetjeneste.tilstand

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.bekreftelsetjeneste.testutils.timestamp
import java.time.Duration
import java.time.Instant

class BekreftelsePeriodeSluttTidsKalkulator: FreeSpec({
    val `14 dagers intervall` = Duration.ofDays(14)
    "Når start er en mandag, skal stopp bli mandag ${`14 dagers intervall`.toDays()} dager senere" - {
        "Når start er mandag 1. juli, blir stopp søndag 14. juli" {
            sluttTidForBekreftelsePeriode(
                startTid = Instant.parse("2024-07-01T08:01:21Z"),
                interval = `14 dagers intervall`
            ) shouldBe Instant.parse("2024-07-14T22:00:00Z")
        }
        "Når start er mandag 25. november, blir stopp søndag 8. desember" {
            sluttTidForBekreftelsePeriode(
                startTid = Instant.parse("2024-11-25T18:01:21Z"),
                interval = `14 dagers intervall`
            ) shouldBe Instant.parse("2024-12-08T23:00:00Z")
        }
        "Når start mandag 21. oktober, blir stopp søndag 3. november" {
            sluttTidForBekreftelsePeriode(
                startTid = Instant.parse("2024-10-21T18:01:21Z"),
                interval = `14 dagers intervall`
            ) shouldBe "04.11.2024 00:00".timestamp
        }
    }
    "Når start ikke er en mandag, skal stopp bli 00:00:00 CET/CEST en mandag ${`14 dagers intervall`.toDays()+1}-${`14 dagers intervall`.toDays()+6} dager senere" - {
        "Når start er onsdag 3. juli, blir stopp søndag 14. juli" {
            sluttTidForBekreftelsePeriode(
                startTid = Instant.parse("2024-07-03T23:01:21Z"),
                interval = `14 dagers intervall`
            ) shouldBe Instant.parse("2024-07-14T22:00:00Z")
        }
        "Når start er 29. mars blir stopp 7. april" {
            sluttTidForBekreftelsePeriode(
                startTid = "29.03.2024 01:21".timestamp,
                interval = `14 dagers intervall`
            ) shouldBe "08.04.2024 00:00".timestamp
        }
    }
})