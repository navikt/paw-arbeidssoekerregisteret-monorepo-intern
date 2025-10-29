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

class GenereringAvNesteSomKrysserOverTilVintertidTest : FreeSpec({

})

