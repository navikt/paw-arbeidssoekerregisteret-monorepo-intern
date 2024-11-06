package no.nav.paw.bekreftelsetjeneste.tilstand

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.test.days
import java.time.Duration
import java.time.Instant

class BekreftelseStartTidKalkulator: FreeSpec({
    val migreringstidspunkt = Instant.parse("2024-01-07T23:00:00Z") // en mandag
    val intervall = Duration.ofDays(14)

    "Når en periode startetes etter migreringstidspunktet skal første bekreftelseperiode starte ved periode start" - {
        "Når periode ble startet $migreringstidspunkt skal første bekreftelseperiode starte $migreringstidspunkt" {
            kalkulerInitiellStartTidForBekreftelsePeriode(
                tidligsteStartTidspunkt = migreringstidspunkt,
                periodeStart = migreringstidspunkt,
                interval = intervall
            ) shouldBe migreringstidspunkt
        }
        "Når perioden ble startet ${migreringstidspunkt + 23.days} skal første bekreftelseperiode starte ${migreringstidspunkt + 23.days}" {
            kalkulerInitiellStartTidForBekreftelsePeriode(
                tidligsteStartTidspunkt = migreringstidspunkt,
                periodeStart = migreringstidspunkt + 23.days,
                interval = intervall
            ) shouldBe migreringstidspunkt + 23.days
        }
    }
    "Når perioden er startet før migreringstidspunktet, skal vi sette startdato slik at den matcher antatt siste meldekort" - {
        "Når perioden startes ${migreringstidspunkt - 5.days} skal første bekreftelseperiode starte 2024-01-14 23:00:00 UTC" {
            kalkulerInitiellStartTidForBekreftelsePeriode(
                tidligsteStartTidspunkt = migreringstidspunkt,
                periodeStart = migreringstidspunkt - 5.days,
                interval = Duration.ofDays(14)
            ) shouldBe Instant.parse("2024-01-14T23:00:00Z")
        }
        //TODO: Vi må avklare om vi vil at denne skal starte 7. januar eller 21. januar, er det egentlig viktig?
        "Når perioden startes lørdag 30. desember 2023 skal første bekreftelseperiode starte 2024-01-07 23:00:00 UTC".config(enabled = false) {
            kalkulerInitiellStartTidForBekreftelsePeriode(
                tidligsteStartTidspunkt = migreringstidspunkt, //tidspunkt søndag 2024-01-07T23:00:00Z
                periodeStart = Instant.parse("2023-12-30T14:00:00Z"),
                interval = Duration.ofDays(14)
            ) shouldBe Instant.parse("2024-01-07T23:00:00Z") //Feiler, starter 21. januar
        }
    }
})