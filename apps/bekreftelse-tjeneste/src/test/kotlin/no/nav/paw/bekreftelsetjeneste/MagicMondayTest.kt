package no.nav.paw.bekreftelsetjeneste

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.bekreftelsetjeneste.tilstand.fristForNesteBekreftelse
import no.nav.paw.bekreftelsetjeneste.tilstand.magicMonday
import java.time.Duration
import java.time.Instant

class MagicMondayTest : FreeSpec({
    val fourteenDaysInterval = Duration.ofDays(14)
    val sevenDaysInterval = Duration.ofDays(7)

    val expectedDatetimeWithFourteenDaysInterval = "2024-01-15T08:00:00Z"
    val expectedDatetimeWithSevenDaysInterval = "2024-01-08T08:00:00Z"


    "magicMonday() returnerer riktig mandag når" - {

        "interval er 14 dager og periode start er mandag uke 1" {
            // mandag uke 1
            val periodeStart = Instant.parse("2024-01-01T08:00:00Z")
            val magicMonday = magicMonday(periodeStart, fourteenDaysInterval)

            // magic monday burde være mandag uke 3
            magicMonday.toString() shouldBe expectedDatetimeWithFourteenDaysInterval
        }

        "interval er 14 dager og periode start er onsdag uke 1" {
            // onsdag uke 1
            val periodeStart = Instant.parse("2024-01-03T08:00:00Z")
            val magicMonday = magicMonday(periodeStart, fourteenDaysInterval)

            // magic monday burde være mandag uke 3
            magicMonday.toString() shouldBe expectedDatetimeWithFourteenDaysInterval
        }

        "interval er 14 dager og periode start er søndag uke 1" {
            // søndag uke 1
            val periodeStart = Instant.parse("2024-01-07T08:00:00Z")
            val magicMonday = magicMonday(periodeStart, fourteenDaysInterval)

            // magic monday burde være mandag uke 3
            magicMonday.toString() shouldBe expectedDatetimeWithFourteenDaysInterval
        }

        "interval er 7 dager og periode start er mandag uke 1" {
            // mandag uke 1
            val periodeStart = Instant.parse("2024-01-01T08:00:00Z")
            val magicMonday = magicMonday(periodeStart, sevenDaysInterval)

            // magic monday burde være mandag uke 2
            magicMonday.toString() shouldBe expectedDatetimeWithSevenDaysInterval
        }

        "interval er 7 dager og periode start er søndag uke 1" {
            // søndag uke 1
            val periodeStart = Instant.parse("2024-01-07T08:00:00Z")
            val magicMonday = magicMonday(periodeStart, sevenDaysInterval)

            // magic monday burde være mandag uke 2
            magicMonday.toString() shouldBe expectedDatetimeWithSevenDaysInterval
        }
    }

    "fristForNesteBekreftelse() returnerer riktig mandag" {
        // mandag uke 1
        val periodeStart = Instant.parse("2024-01-01T08:00:00Z")
        val frist = fristForNesteBekreftelse(periodeStart, fourteenDaysInterval)

        // frist burde være mandag uke 3
        frist.toString() shouldBe expectedDatetimeWithFourteenDaysInterval
    }
})