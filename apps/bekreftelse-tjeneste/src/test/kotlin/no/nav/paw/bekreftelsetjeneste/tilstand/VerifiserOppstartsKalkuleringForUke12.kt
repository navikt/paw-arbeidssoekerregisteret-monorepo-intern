package no.nav.paw.bekreftelsetjeneste.tilstand

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import java.time.Duration
import java.time.LocalDateTime.parse
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

val norskTid = ZoneId.of("Europe/Oslo")

fun String.tidspunkt(): ZonedDateTime = parse(this).atZone(norskTid)
private val prettyPrintFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("cccc dd.MM 'uke' w")
val ZonedDateTime.dagOgUke: String get() = format(prettyPrintFormat)
    .replace("Monday", "Mandag")
    .replace("Tuesday", "Tirsdag")
    .replace("Wednesday", "Onsdag")
    .replace("Thursday", "Torsdag")
    .replace("Friday", "Fredag")
    .replace("Saturday", "Lørdag")
    .replace("Sunday", "Søndag")


class VerifiserOppstartsKalkuleringForUke12 : FreeSpec({
    val intervall = Duration.ofDays(14)
    "Verifiser første bekreftelse periode ved ${intervall.toDays()} dagers intervall" - {
        listOf(
            case(
                migreringstidspunkt = "2024-01-07T23:00:00".tidspunkt(),
                periodeStart = "2025-03-07T00:00:00".tidspunkt(),
                forventetStart = "2025-03-07T00:00:00".tidspunkt(),
                forventetSlutt = "2025-03-17T00:00:00".tidspunkt()
            ),
            case(
                migreringstidspunkt = "2025-03-10T00:00:00".tidspunkt(),
                periodeStart = "2025-02-04T00:00:00".tidspunkt(),
                forventetStart = "2025-03-17T00:00:00".tidspunkt(),
                forventetSlutt = "2025-03-31T00:00:00".tidspunkt()
            ),
        ).forEach { (migreringstidspunkt, periodeStart, forventetStart, forventetSlutt) ->
            "Ved siste Arena publisering ${migreringstidspunkt.dagOgUke} og periode start ${periodeStart.dagOgUke}" - {
                val startTid = kalkulerInitiellStartTidForBekreftelsePeriode(
                    tidligsteStartTidspunkt = migreringstidspunkt.toInstant(),
                    periodeStart = periodeStart.toInstant(),
                    interval = Duration.ofDays(14)
                )
                "start skal være ${forventetStart.dagOgUke} ($forventetStart)" {
                    startTid shouldBe forventetStart.toInstant()
                }
                "slutt skal være ${forventetSlutt.dagOgUke} (${forventetSlutt}" {
                    sluttTidForBekreftelsePeriode(startTid, intervall)
                }
            }
        }
    }
})

data class TestCase(
    val migreringstidspunkt: ZonedDateTime,
    val periodeStart: ZonedDateTime,
    val forventetStart: ZonedDateTime,
    val forventetSlutt: ZonedDateTime
)


fun case(
    migreringstidspunkt: ZonedDateTime = "2025-03-04T00:00:00".tidspunkt(),
    periodeStart: ZonedDateTime,
    forventetStart: ZonedDateTime,
    forventetSlutt: ZonedDateTime
): TestCase = TestCase(
    migreringstidspunkt = migreringstidspunkt,
    periodeStart = periodeStart,
    forventetStart = forventetStart,
    forventetSlutt = forventetSlutt
)