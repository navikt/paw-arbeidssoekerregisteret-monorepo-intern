package no.nav.paw.bekreftelsetjeneste.tilstand

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime

class SluttTidForBekreftelsePeriodeTest : FreeSpec({
    val testCases = listOf(
        test(
            startTid = "2025-02-03T00:00:00+01:00[Europe/Oslo]",
            interval = 14.dager,
            forventetSluttTid = "2025-02-17T00:00:00+01:00[Europe/Oslo]"
        ),
        test(
            startTid = "2025-03-24T00:00:00+01:00[Europe/Oslo]", // before DST
            interval = 14.dager,
            forventetSluttTid = "2025-04-07T00:00:00+02:00[Europe/Oslo]" // after DST
        ),
        test(
            startTid = "2025-10-20T00:00:00+02:00[Europe/Oslo]", // before winter time
            interval = 14.dager,
            forventetSluttTid = "2025-11-03T00:00:00+01:00[Europe/Oslo]" // after winter time
        ),
        test(
            startTid = "2025-06-02T00:00:00+02:00[Europe/Oslo]", // summer time
            interval = 14.dager,
            forventetSluttTid = "2025-06-16T00:00:00+02:00[Europe/Oslo]"
        ),
        test(
            startTid = "2025-12-29T00:00:00+01:00[Europe/Oslo]", // crossing new year
            interval = 14.dager,
            forventetSluttTid = "2026-01-12T00:00:00+01:00[Europe/Oslo]"
        ),
        test(
            startTid = "2024-02-26T00:00:00+01:00[Europe/Oslo]", // last Monday of Feb, leap year
            interval = 14.dager,
            forventetSluttTid = "2024-03-11T00:00:00+01:00[Europe/Oslo]"
        ),
        test(
            startTid = "2024-03-25T00:00:00+01:00[Europe/Oslo]", // starts on DST change day
            interval = 14.dager,
            forventetSluttTid = "2024-04-08T00:00:00+02:00[Europe/Oslo]"
        ),
        test(
            startTid = "2024-10-21T00:00:00+02:00[Europe/Oslo]", // ends on DST change day
            interval = 14.dager,
            forventetSluttTid = "2024-11-04T00:00:00+01:00[Europe/Oslo]"
        ),
        test(
            startTid = "2024-02-19T00:00:00+01:00[Europe/Oslo]", // crossing leap day
            interval = 14.dager,
            forventetSluttTid = "2024-03-04T00:00:00+01:00[Europe/Oslo]"
        ),
        test(
            startTid = "2025-11-03T00:00:00+01:00[Europe/Oslo]", // first Monday of November
            interval = 14.dager,
            forventetSluttTid = "2025-11-17T00:00:00+01:00[Europe/Oslo]"
        ),
        test(
            startTid = "2025-08-25T00:00:00+02:00[Europe/Oslo]", // crossing month boundary
            interval = 14.dager,
            forventetSluttTid = "2025-09-08T00:00:00+02:00[Europe/Oslo]"
        )
    )
    "sluttTidForBekreftelsePeriode" - {
        testCases.forEach { testCase ->
            "Start tid ${testCase.startTid} med interval ${testCase.interval.toDays()} dager skal gi sluttTid=${testCase.forventetSluttTid} " {
                val startTid = ZonedDateTime.parse(testCase.startTid).toInstant()
                val forventetSluttTid = ZonedDateTime.parse(testCase.forventetSluttTid).toInstant()
                val beregnetSluttTid: Instant = sluttTidForBekreftelsePeriode(
                    startTid = startTid,
                    interval = testCase.interval
                )
                beregnetSluttTid shouldBe forventetSluttTid
            }
        }
    }
})

data class PeriodeTestCase(
    val startTid: String,
    val interval: Duration,
    val forventetSluttTid: String,
)

fun test(
    startTid: String,
    interval: Duration,
    forventetSluttTid: String
) = PeriodeTestCase(
    startTid = startTid, interval = interval,
    forventetSluttTid = forventetSluttTid
)

val Int.dager get() = Duration.ofDays(this.toLong())


