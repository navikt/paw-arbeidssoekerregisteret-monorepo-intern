package no.nav.paw.bekreftelsetjeneste.topology

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.bekreftelsetjeneste.config.BekreftelseKonfigurasjon
import no.nav.paw.bekreftelsetjeneste.paavegneav.WallClock
import no.nav.paw.bekreftelsetjeneste.tilstand.BekreftelseTilstand
import no.nav.paw.bekreftelsetjeneste.tilstand.PeriodeInfo

import java.time.Duration
import java.time.Instant
import java.time.Instant.parse
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

val norskTid = ZoneId.of("Europe/Oslo")

class GenereringAvFoersteBekreftelseTest : FreeSpec({
    val tidligste = LocalDate.parse("2025-03-10")
    fun String.vedStartAvDagen(): Instant = LocalDate.parse(this).atStartOfDay(norskTid).toInstant()
    fun tidligstePlussUker(uker: Long) = tidligste.plusWeeks(uker).atStartOfDay(norskTid).toInstant()
    val map = DummyOddetallPartallMap()
    val intervall = Duration.ofDays(14)
    val bekreftelseKonfigurasjon = BekreftelseKonfigurasjon(
        maksAntallVentendeBekreftelser = 3,
        tidligsteBekreftelsePeriodeStart = tidligste,
        interval = intervall,
        graceperiode = Duration.ofDays(7),
        tilgjengeligOffset = Duration.ofDays(3)
    )

    "Når ingen bekreftelser er laget og perioden startet før tidligste tidspunkt" - {
        "og migreringsdato ikke er nådd skal det opprettes en bekreftelse men ikke publiseres noe" - {
            "dersom personen er på oddetallssuker skal første periode starte $tidligste" {
                val periodeInfo = periodeInfo(
                    identitetsnummer = "12345678901",
                    startet = parse("2024-03-03T15:26:00Z")
                )
                val context = BekreftelseContext(
                    konfigurasjon = bekreftelseKonfigurasjon,
                    wallClock = WallClock(parse("2024-03-03T15:26:00Z")),
                    periodeInfo = periodeInfo,
                    oddetallPartallMap = map,
                    prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
                )
                val (tilstand, _, hendelser) = context.prosesser(BekreftelseTilstand(0, periodeInfo, emptyList()))
                tilstand.bekreftelser.size shouldBe 2
                tilstand.bekreftelser.maxBy { it.gjelderFra } should { bekreftelse ->
                    bekreftelse.gjelderFra shouldBe "2025-03-10".vedStartAvDagen()
                    bekreftelse.gjelderTil shouldBe "2025-03-24".vedStartAvDagen()
                }
                tilstand.bekreftelser.minBy { it.gjelderFra } should { bekreftelse ->
                    bekreftelse.gjelderFra shouldBe "2025-02-24".vedStartAvDagen()
                    bekreftelse.gjelderTil shouldBe "2025-03-10".vedStartAvDagen()
                }
                hendelser.shouldBeEmpty()
            }
            "dersom personen er på ukjent ukenummer skal første periode starte $tidligste" {
                val periodeInfo = periodeInfo(
                    identitetsnummer = "12345678909",
                    startet = parse("2024-03-03T15:26:00Z")
                )
                val context = BekreftelseContext(
                    konfigurasjon = bekreftelseKonfigurasjon,
                    wallClock = WallClock(parse("2024-03-03T15:26:00Z")),
                    periodeInfo = periodeInfo,
                    oddetallPartallMap = map,
                    prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
                )
                val (tilstand, _, hendelser) = context.prosesser(BekreftelseTilstand(0, periodeInfo, emptyList()))
                tilstand.bekreftelser.size shouldBe 2
                tilstand.bekreftelser.maxBy { it.gjelderFra } should { bekreftelse ->
                    bekreftelse.gjelderFra shouldBe "2025-03-10".vedStartAvDagen()
                    bekreftelse.gjelderTil shouldBe "2025-03-24".vedStartAvDagen()
                }
                tilstand.bekreftelser.minBy { it.gjelderFra } should { bekreftelse ->
                    bekreftelse.gjelderFra shouldBe "2025-02-24".vedStartAvDagen()
                    bekreftelse.gjelderTil shouldBe "2025-03-10".vedStartAvDagen()
                }
                hendelser.shouldBeEmpty()
            }
            "dersom personen er på partallsuker skal første periode starte ${tidligstePlussUker(1)}" {
                val periodeInfo = periodeInfo(
                    identitetsnummer = "12345678902",
                    startet = parse("2024-03-03T15:26:00Z")
                )
                val context = BekreftelseContext(
                    konfigurasjon = bekreftelseKonfigurasjon,
                    wallClock = WallClock(parse("2024-03-03T15:26:00Z")),
                    periodeInfo = periodeInfo,
                    oddetallPartallMap = map,
                    prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
                )
                val (tilstand, _, hendelser) = context.prosesser(BekreftelseTilstand(0, periodeInfo, emptyList()))
                tilstand.bekreftelser.size shouldBe 2
                tilstand.bekreftelser.maxBy { it.gjelderFra } should { bekreftelse ->
                    bekreftelse.gjelderFra shouldBe "2025-03-17".vedStartAvDagen()
                    bekreftelse.gjelderTil shouldBe "2025-03-31".vedStartAvDagen()
                }
                tilstand.bekreftelser.minBy { it.gjelderFra } should { bekreftelse ->
                    bekreftelse.gjelderFra shouldBe "2025-02-17".vedStartAvDagen()
                    bekreftelse.gjelderTil shouldBe "2025-03-03".vedStartAvDagen()
                }
                hendelser.shouldBeEmpty()
            }
        }
        "og migreringtidspunktet er nådd" - {
            "dersom personen er på oddetallssuker skal første periode starte ved periode start" {
                val periodeInfo = periodeInfo(
                    identitetsnummer = "12345678901",
                    startet = parse("2024-03-03T15:26:00Z")
                )
                val context = BekreftelseContext(
                    konfigurasjon = bekreftelseKonfigurasjon,
                    wallClock = WallClock(parse("2025-03-18T15:26:00Z")),
                    periodeInfo = periodeInfo,
                    oddetallPartallMap = map,
                    prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
                )
                val (tilstand, _, hendelser) = context.prosesser(BekreftelseTilstand(0, periodeInfo, emptyList()))
                tilstand.bekreftelser.size shouldBe 2
                tilstand.bekreftelser.maxBy { it.gjelderFra } should { bekreftelse ->
                    bekreftelse.gjelderFra shouldBe "2025-03-10".vedStartAvDagen()
                    bekreftelse.gjelderTil shouldBe "2025-03-24".vedStartAvDagen()
                }
                tilstand.bekreftelser.minBy { it.gjelderFra } should { bekreftelse ->
                    bekreftelse.gjelderFra shouldBe "2025-02-24".vedStartAvDagen()
                    bekreftelse.gjelderTil shouldBe "2025-03-10".vedStartAvDagen()
                }
                hendelser.shouldBeEmpty()
            }
            "dersom personen er på ukjent ukenummer skal første periode starte $tidligste" {
                val periodeInfo = periodeInfo(
                    identitetsnummer = "12345678909",
                    startet = parse("2024-03-03T15:26:00Z")
                )
                val context = BekreftelseContext(
                    konfigurasjon = bekreftelseKonfigurasjon,
                    wallClock = WallClock(parse("2024-03-03T15:26:00Z")),
                    periodeInfo = periodeInfo,
                    oddetallPartallMap = map,
                    prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
                )
                val (tilstand, _, hendelser) = context.prosesser(BekreftelseTilstand(0, periodeInfo, emptyList()))
                tilstand.bekreftelser.size shouldBe 2
                tilstand.bekreftelser.maxBy { it.gjelderFra } should { bekreftelse ->
                    bekreftelse.gjelderFra shouldBe "2025-03-10".vedStartAvDagen()
                    bekreftelse.gjelderTil shouldBe "2025-03-24".vedStartAvDagen()
                }
                tilstand.bekreftelser.minBy { it.gjelderFra } should { bekreftelse ->
                    bekreftelse.gjelderFra shouldBe "2025-02-24".vedStartAvDagen()
                    bekreftelse.gjelderTil shouldBe "2025-03-10".vedStartAvDagen()
                }
                hendelser.shouldBeEmpty()
            }
            "dersom personen er på partallsuker skal første periode starte ved periode start" {
                val periodeInfo = periodeInfo(
                    identitetsnummer = "12345678902",
                    startet = parse("2024-03-03T15:26:00Z")
                )
                val context = BekreftelseContext(
                    konfigurasjon = bekreftelseKonfigurasjon,
                    wallClock = WallClock(parse("2025-03-18T23:01:00Z")),
                    periodeInfo = periodeInfo,
                    oddetallPartallMap = map,
                    prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
                )
                val (tilstand, _, hendelser) = context.prosesser(BekreftelseTilstand(0, periodeInfo, emptyList()))
                tilstand.bekreftelser.size shouldBe 2
                tilstand.bekreftelser.maxBy { it.gjelderFra } should { bekreftelse ->
                    bekreftelse.gjelderFra shouldBe "2025-03-17".vedStartAvDagen()
                    bekreftelse.gjelderTil shouldBe "2025-03-31".vedStartAvDagen()
                }
                tilstand.bekreftelser.minBy { it.gjelderFra } should { bekreftelse ->
                    bekreftelse.gjelderFra shouldBe "2025-02-17".vedStartAvDagen()
                    bekreftelse.gjelderTil shouldBe "2025-03-03".vedStartAvDagen()
                }
                hendelser.shouldBeEmpty()
            }
        }
        "og perioden startet etter migreringstidspunktet" - {
            "dersom personen er på oddetallssuker skal første periode starte ved periode start" {
                val periodeInfo = periodeInfo(
                    identitetsnummer = "12345678901",
                    startet = parse("2025-03-09T23:00:00Z")
                )
                val context = BekreftelseContext(
                    konfigurasjon = bekreftelseKonfigurasjon,
                    wallClock = WallClock(parse("2025-03-18T15:26:00Z")),
                    periodeInfo = periodeInfo,
                    oddetallPartallMap = map,
                    prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
                )
                val (tilstand, _, hendelser) = context.prosesser(BekreftelseTilstand(0, periodeInfo, emptyList()))
                tilstand.bekreftelser.size shouldBe 2
                tilstand.bekreftelser.first() should { bekreftelse ->
                    bekreftelse.gjelderFra shouldBe periodeInfo.startet
                    bekreftelse.gjelderTil shouldBe "2025-03-24".vedStartAvDagen()
                }
                tilstand.bekreftelser.minBy { it.gjelderFra } should { bekreftelse ->
                    bekreftelse.gjelderFra shouldBe "2025-02-24".vedStartAvDagen()
                    bekreftelse.gjelderTil shouldBe "2025-03-10".vedStartAvDagen()
                }
                hendelser.shouldBeEmpty()
            }
            "dersom personen er på ukjent ukenummer skal første periode starte ved periode start" {
                val periodeInfo = periodeInfo(
                    identitetsnummer = "12345678909",
                    startet = parse("2025-03-18T15:26:00Z")
                )
                val context = BekreftelseContext(
                    konfigurasjon = bekreftelseKonfigurasjon,
                    wallClock = WallClock(parse("2025-03-18T15:26:00Z")),
                    periodeInfo = periodeInfo,
                    oddetallPartallMap = map,
                    prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
                )
                val (tilstand, _, hendelser) =
                    context.prosesser(BekreftelseTilstand(0, periodeInfo, emptyList()))
                tilstand.bekreftelser.size shouldBe 1
                tilstand.bekreftelser.first() should { bekreftelse ->
                    bekreftelse.gjelderFra shouldBe periodeInfo.startet
                    bekreftelse.gjelderTil shouldBe "2025-03-31".vedStartAvDagen()
                }
                hendelser.shouldBeEmpty()
            }
            "dersom personen er på partallsuker skal første periode starte ved periode start" {
                val periodeInfo = periodeInfo(
                    identitetsnummer = "12345678902",
                    startet = parse("2025-03-14T15:26:00Z")
                )
                val context = BekreftelseContext(
                    konfigurasjon = bekreftelseKonfigurasjon,
                    wallClock = WallClock(parse("2025-03-18T23:01:00Z")),
                    periodeInfo = periodeInfo,
                    oddetallPartallMap = map,
                    prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
                )
                val (tilstand, _, hendelser) =
                    context.prosesser(BekreftelseTilstand(0, periodeInfo, emptyList()))
                tilstand.bekreftelser.size shouldBe 1
                tilstand.bekreftelser.first() should { bekreftelse ->
                    bekreftelse.gjelderFra shouldBe periodeInfo.startet
                    bekreftelse.gjelderTil shouldBe "2025-03-24".vedStartAvDagen()
                }
                hendelser.shouldBeEmpty()
            }
        }
    }
})

fun periodeInfo(
    identitetsnummer: String = "12345678901",
    startet: Instant = parse("2024-03-03T15:26:00Z"),
    periodeId: UUID = UUID.randomUUID(),
    arbeidsoekerId: Long = 1L,
    recordKey: Long = 0L,
    avsluttet: Instant? = null,
): PeriodeInfo = PeriodeInfo(
    identitetsnummer = identitetsnummer,
    startet = startet,
    periodeId = periodeId,
    arbeidsoekerId = arbeidsoekerId,
    recordKey = recordKey,
    avsluttet = avsluttet
)