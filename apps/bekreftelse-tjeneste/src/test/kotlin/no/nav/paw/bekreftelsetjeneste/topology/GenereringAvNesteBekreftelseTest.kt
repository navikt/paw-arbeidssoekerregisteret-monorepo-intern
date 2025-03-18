package no.nav.paw.bekreftelsetjeneste.topology

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.bekreftelsetjeneste.config.BekreftelseKonfigurasjon
import no.nav.paw.bekreftelsetjeneste.paavegneav.WallClock
import no.nav.paw.bekreftelsetjeneste.testutils.timestamp
import no.nav.paw.bekreftelsetjeneste.tilstand.Bekreftelse
import no.nav.paw.bekreftelsetjeneste.tilstand.BekreftelseTilstandsLogg
import no.nav.paw.bekreftelsetjeneste.tilstand.IkkeKlarForUtfylling
import no.nav.paw.bekreftelsetjeneste.tilstand.VenterSvar
import no.nav.paw.bekreftelsetjeneste.tilstand.sisteTilstand
import no.nav.paw.collections.pawNonEmptyListOf

import java.time.Duration
import java.time.Instant.parse
import java.time.LocalDate
import java.util.*

class GenereringAvNesteBekreftelseTest : FreeSpec({
    val tidligste = LocalDate.parse("2023-03-10")
    val map = DummyOddetallPartallMap()
    val intervall = Duration.ofDays(14)
    val bekreftelseKonfigurasjon = BekreftelseKonfigurasjon(
        maksAntallVentendeBekreftelser = 3,
        tidligsteBekreftelsePeriodeStart = tidligste,
        interval = intervall,
        graceperiode = Duration.ofDays(7),
        tilgjengeligOffset = Duration.ofDays(3)
    )
    val periodeInfo = periodeInfo(
        startet = parse("2025-03-11T12:00:00Z")
    )
    fun context(tidspunkt: String): BekreftelseContext {
        return BekreftelseContext(
            konfigurasjon = bekreftelseKonfigurasjon,
            wallClock = WallClock(tidspunkt.timestamp),
            periodeInfo = periodeInfo,
            oddetallPartallMap = map,
            prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
        )
    }
    "Vi skal opprette ny bekreftelse når" - {
        "fristen for siste er passert og det er mindre en utestående bekreftelse" {
            val oppdatertList = context("14.03.2025 12:12")
                .opprettManglendeBekreftelser(pawNonEmptyListOf(
                    Bekreftelse(
                        tilstandsLogg = BekreftelseTilstandsLogg(VenterSvar("13.03.2025 12:00".timestamp), emptyList()),
                        bekreftelseId = UUID.randomUUID(),
                        gjelderFra = "24.02.2025 10:10".timestamp,
                        gjelderTil = "10.03.2025 00:00".timestamp,
                    )
                ))
            oppdatertList.size shouldBe 2
            val siste = oppdatertList.maxBy { it.gjelderTil }
            siste.gjelderFra shouldBe "10.03.2025 00:00".timestamp
            siste.gjelderTil shouldBe "24.03.2025 00:00".timestamp
            siste.sisteTilstand().shouldBeInstanceOf<IkkeKlarForUtfylling>()
        }
    }
})

