package no.nav.paw.bekreftelsetjeneste.topology

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.date.plusOrMinus
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.bekreftelsetjeneste.config.BekreftelseKonfigurasjon
import no.nav.paw.bekreftelsetjeneste.paavegneav.WallClock
import no.nav.paw.bekreftelsetjeneste.tilstand.Bekreftelse
import no.nav.paw.bekreftelsetjeneste.tilstand.BekreftelseTilstand
import no.nav.paw.bekreftelsetjeneste.tilstand.BekreftelseTilstandsLogg
import no.nav.paw.bekreftelsetjeneste.tilstand.IkkeKlarForUtfylling
import no.nav.paw.bekreftelsetjeneste.tilstand.KlarForUtfylling
import no.nav.paw.bekreftelsetjeneste.tilstand.Levert
import no.nav.paw.bekreftelsetjeneste.tilstand.PeriodeInfo
import no.nav.paw.bekreftelsetjeneste.tilstand.sisteTilstand
import no.nav.paw.bekreftelsetjeneste.tilstand.tilstand
import java.time.Duration.ofDays
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.util.UUID

class OpprettManglendeBekreftelserTest : FreeSpec({
    "Nå vi lager en periode som ikke krysser bytte til vintertid blir det riktig" {
        val periodeInfo = PeriodeInfo(
            periodeId = UUID.randomUUID(),
            identitetsnummer = "12345678900",
            arbeidsoekerId = 1L,
            recordKey = 1L,
            startet = Instant.parse("2025-01-01T00:00:00Z"),
            avsluttet = null
        )
        val context = BekreftelseContext(
            prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
            konfigurasjon = BekreftelseKonfigurasjon(
                tidligsteBekreftelsePeriodeStart = LocalDate.parse("2021-01-01"),
                interval = ofDays(14),
                graceperiode = ofDays(7),
                tilgjengeligOffset = ofDays(2),
            ),
            wallClock = WallClock(Instant.parse("2025-10-20T10:00:00Z")),
            periodeInfo = periodeInfo
        )
        val tilstand = BekreftelseTilstand(
            bekreftelser = listOf(
                Bekreftelse(
                    bekreftelseId = UUID.randomUUID(),
                    gjelderFra = Instant.parse("2025-09-14T22:00:00Z"),
                    gjelderTil = Instant.parse("2025-09-28T22:00:00Z"),
                    tilstandsLogg = BekreftelseTilstandsLogg(
                        siste = Levert(Instant.parse("2025-09-28T16:48:23Z")),
                        tidligere = emptyList()
                    )
                )
            ),
            kafkaPartition = 1,
            periode = periodeInfo
        )
        val resultat = context.prosesser(tilstand)
        resultat.oppdatertTilstand.bekreftelser.size shouldBe 2
        resultat.oppdatertTilstand.bekreftelser.find { it.sisteTilstand() is KlarForUtfylling} should { nyBekreftelse ->
            nyBekreftelse.shouldNotBeNull()
            nyBekreftelse.gjelderFra shouldBe Instant.parse("2025-09-28T22:00:00Z")
            nyBekreftelse.gjelderTil shouldBe Instant.parse("2025-10-12T22:00:00Z")
        }
    }

    "Nå vi lager en periode som krysser bytte til vintertid ble det riktig" {
        val periodeInfo = PeriodeInfo(
            periodeId = UUID.randomUUID(),
            identitetsnummer = "12345678900",
            arbeidsoekerId = 1L,
            recordKey = 1L,
            startet = Instant.parse("2025-01-01T00:00:00Z"),
            avsluttet = null
        )
        val context = BekreftelseContext(
            prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
            konfigurasjon = BekreftelseKonfigurasjon(
                tidligsteBekreftelsePeriodeStart = LocalDate.parse("2021-01-01"),
                interval = ofDays(14),
                graceperiode = ofDays(7),
                tilgjengeligOffset = ofDays(2),
            ),
            wallClock = WallClock(Instant.parse("2025-10-20T10:00:00Z")),
            periodeInfo = periodeInfo
        )
        val tilstand = BekreftelseTilstand(
            bekreftelser = listOf(
                Bekreftelse(
                    bekreftelseId = UUID.randomUUID(),
                    gjelderFra = Instant.parse("2025-09-28T22:00:00Z"),
                    gjelderTil = Instant.parse("2025-10-12T22:00:00Z"),
                    tilstandsLogg = BekreftelseTilstandsLogg(
                        siste = Levert(Instant.parse("2025-10-11T16:48:23Z")),
                        tidligere = emptyList()
                    )
                )
            ),
            kafkaPartition = 1,
            periode = periodeInfo
        )
        val resultat = context.prosesser(tilstand)
        resultat.oppdatertTilstand.bekreftelser.size shouldBe 2
        resultat.oppdatertTilstand.bekreftelser.find { it.sisteTilstand() is IkkeKlarForUtfylling} should { nyBekreftelse ->
            nyBekreftelse.shouldNotBeNull()
            nyBekreftelse.gjelderFra shouldBe Instant.parse("2025-10-12T22:00:00Z")
            nyBekreftelse.gjelderTil shouldBe Instant.parse("2025-10-26T23:00:00Z")
        }
    }

    "Nå vi lager en periode som etter bytte til vintertid blir det riktig" {
        val periodeInfo = PeriodeInfo(
            periodeId = UUID.randomUUID(),
            identitetsnummer = "12345678900",
            arbeidsoekerId = 1L,
            recordKey = 1L,
            startet = Instant.parse("2025-01-01T00:00:00Z"),
            avsluttet = null
        )
        val context = BekreftelseContext(
            prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
            konfigurasjon = BekreftelseKonfigurasjon(
                tidligsteBekreftelsePeriodeStart = LocalDate.parse("2021-01-01"),
                interval = ofDays(14),
                graceperiode = ofDays(7),
                tilgjengeligOffset = ofDays(2),
            ),
            wallClock = WallClock(Instant.parse("2025-10-27T10:00:00Z")),
            periodeInfo = periodeInfo
        )
        val tilstand = BekreftelseTilstand(
            bekreftelser = listOf(
                Bekreftelse(
                    bekreftelseId = UUID.randomUUID(),
                    gjelderFra = Instant.parse("2025-10-12T22:00:00Z"),
                    gjelderTil = Instant.parse("2025-10-26T23:00:00Z"),
                    tilstandsLogg = BekreftelseTilstandsLogg(
                        siste = Levert(Instant.parse("2025-10-25T16:48:23Z")),
                        tidligere = emptyList()
                    )
                )
            ),
            kafkaPartition = 1,
            periode = periodeInfo
        )
        val resultat = context.prosesser(tilstand)
        withClue(resultat.oppdatertTilstand.bekreftelser.joinToString("\n")) {
            resultat.oppdatertTilstand.bekreftelser.size shouldBe 2
        }
        resultat.oppdatertTilstand.bekreftelser.find { it.sisteTilstand() is IkkeKlarForUtfylling} should { nyBekreftelse ->
            nyBekreftelse.shouldNotBeNull()
            nyBekreftelse.gjelderFra shouldBe Instant.parse("2025-10-26T23:00:00Z")
            nyBekreftelse.gjelderTil shouldBe Instant.parse("2025-11-09T23:00:00Z")
        }
    }
})
