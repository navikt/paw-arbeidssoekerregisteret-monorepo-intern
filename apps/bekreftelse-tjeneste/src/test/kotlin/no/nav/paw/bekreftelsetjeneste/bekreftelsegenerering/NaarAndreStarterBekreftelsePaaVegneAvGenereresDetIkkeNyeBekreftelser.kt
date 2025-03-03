package no.nav.paw.bekreftelsetjeneste.bekreftelsegenerering

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.bekreftelsetjeneste.paavegneav.InternPaaVegneAv
import no.nav.paw.bekreftelsetjeneste.paavegneav.Loesning
import no.nav.paw.bekreftelsetjeneste.paavegneav.WallClock
import no.nav.paw.bekreftelsetjeneste.paavegneav.opprettPaaVegneAvTilstand
import no.nav.paw.bekreftelsetjeneste.testutils.bekreftelse
import no.nav.paw.bekreftelsetjeneste.testutils.bekreftelseTilstand
import no.nav.paw.bekreftelsetjeneste.testutils.standardIntervaller
import no.nav.paw.bekreftelsetjeneste.topology.prosesserBekreftelseOgPaaVegneAvTilstand
import no.nav.paw.test.days
import no.nav.paw.test.seconds
import java.time.Instant

class NaarAndreStarterBekreftelsePaaVegneAvGenereresDetIkkeNyeBekreftelser : FreeSpec({
    val intervaller = standardIntervaller
    val periodeStart = Instant.parse("2024-10-27T18:00:00Z")
    val bekreftelseTilstand = bekreftelseTilstand(periodeStart = periodeStart)
    val paaVegneAvTilstand = opprettPaaVegneAvTilstand(
        periodeId = bekreftelseTilstand.periode.periodeId,
        paaVegneAv = InternPaaVegneAv(
            loesning = Loesning.DAGPENGER,
            intervall = intervaller.interval,
            gracePeriode = intervaller.graceperiode
        )
    )
    "Når andre har startet bekreftelsePaaVegneAv og det ikke finnes noen bekreftelser skal det ikke genereres nye bekreftelser når " - {
        listOf(
            1.seconds,
            1.days,
            10.days,
            20.days,
            50.days
        ).forEach { tidEtterStart ->
            "bekreftelse puntuator kjører $tidEtterStart etter start av periode" - {
                val resultat = sequenceOf(bekreftelseTilstand to paaVegneAvTilstand)
                    .prosesserBekreftelseOgPaaVegneAvTilstand(
                        bekreftelseKonfigurasjon = intervaller,
                        wallClock = WallClock(periodeStart + tidEtterStart)
                    ).toList()
                "Ingen ting skal skje" {
                    withClue("Forventet ingen elementer, men det var ${resultat.size}: $resultat") {
                        resultat.isEmpty() shouldBe true
                    }
                }
            }
        }
    }
    "Når andre har startet bekreftelsePaaVegneAv og det finnes en ikke besvart bekreftelse skal det ikke skje noe " - {
        listOf(
            1.seconds,
            1.days,
            10.days,
            20.days,
            50.days
        ).forEach { tidEtterStart ->
            "bekreftelse puntuator kjører $tidEtterStart etter start av periode" - {
                val resultat = sequenceOf(
                    bekreftelseTilstand.copy(
                        bekreftelser = listOf(
                            intervaller.bekreftelse(
                                gjelderFra = periodeStart,
                                gracePeriodeUtloept = null,
                                gracePeriodeVarselet = null
                            )
                        )
                    ) to paaVegneAvTilstand
                )
                    .prosesserBekreftelseOgPaaVegneAvTilstand(
                        bekreftelseKonfigurasjon = intervaller,
                        wallClock = WallClock(periodeStart + tidEtterStart)
                    ).toList()
                "Ingen ting skal skje" {
                    withClue("Forventet ingen elementer, men det var ${resultat.size}: $resultat") {
                        resultat.isEmpty() shouldBe true
                    }
                }
            }
        }
    }
})