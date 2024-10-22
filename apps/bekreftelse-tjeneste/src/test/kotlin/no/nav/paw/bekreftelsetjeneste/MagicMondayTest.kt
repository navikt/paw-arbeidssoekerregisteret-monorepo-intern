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
        val frist = fristForNesteBekreftelse(periodeStart, fourteenDaysInterval, periodeStart)

        // frist burde være mandag uke 3
        frist.toString() shouldBe expectedDatetimeWithFourteenDaysInterval

        // mandag uke 2
        val periodeStart2 = Instant.parse("2024-01-08T08:00:00Z")
        val frist2 = fristForNesteBekreftelse(periodeStart2, fourteenDaysInterval, periodeStart2)

        // frist burde være mandag uke 4
        frist2.toString() shouldBe "2024-01-22T08:00:00Z"
    }

    "Gamle perioder gis riktig frist" {
        // mandag uke 1
        val periodeStart = Instant.parse("2024-01-01T08:00:00Z")
        // tirsdag uke 1
        val periodeStart2 = Instant.parse("2024-01-02T08:00:00Z")

        val expectedDatetime = "2024-04-15T08:00:00Z" // mandag uke 16

        val magicMonday = fristForNesteBekreftelse(periodeStart, fourteenDaysInterval,
            Instant.parse("2024-04-04T08:00:00Z") // torsdag uke 14
        )
        magicMonday.toString() shouldBe expectedDatetime

        val magicMonday2 = fristForNesteBekreftelse(periodeStart2, fourteenDaysInterval,
            Instant.parse("2024-04-06T08:00:00Z") // lørdag uke 14
        )
        magicMonday2.toString() shouldBe expectedDatetime

        val magicMonday3 = fristForNesteBekreftelse(periodeStart2, fourteenDaysInterval,
            Instant.parse("2024-04-01T08:00:00Z") // mandag uke 14
        )
        magicMonday3.toString() shouldBe expectedDatetime
    }

    "Gammel periode med start 19.januar med dagens dato 15.mars skal få magic monday 25.mars" {
        val periodeStartFredag19Januar = Instant.parse("2024-01-19T08:00:00Z")
        val magicMondayFraFredag15mars = fristForNesteBekreftelse(periodeStartFredag19Januar, fourteenDaysInterval,
            Instant.parse("2024-03-15T08:00:00Z")
        )

        magicMondayFraFredag15mars.toString() shouldBe "2024-03-25T08:00:00Z"
    }
})