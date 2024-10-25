package no.nav.paw.bekreftelsetjeneste.bekreftelsegenerering

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.bekreftelsetjeneste.ansvar.Ansvarlig
import no.nav.paw.bekreftelsetjeneste.ansvar.Loesning
import no.nav.paw.bekreftelsetjeneste.ansvar.WallClock
import no.nav.paw.bekreftelsetjeneste.ansvar.ansvar
import no.nav.paw.bekreftelsetjeneste.bekreftelse
import no.nav.paw.bekreftelsetjeneste.internTilstand
import no.nav.paw.bekreftelsetjeneste.standardIntervaller
import no.nav.paw.bekreftelsetjeneste.topology.prosessererBekreftelser
import no.nav.paw.test.days
import no.nav.paw.test.seconds
import java.time.Instant

class NaarAndreHarAnsvarGenereresDetIkkeNyBekreftelser : FreeSpec({
    val intervaller = standardIntervaller
    val periodeStart = Instant.parse("2024-10-27T18:00:00Z")
    val internTilstand = internTilstand(periodeStart = periodeStart)
    val ansvar = ansvar(
        periodeId = internTilstand.periode.periodeId,
        ansvarlig = Ansvarlig(
            loesning = Loesning.DAGPENGER,
            intervall = intervaller.interval,
            gracePeriode = intervaller.graceperiode
        )
    )
    "Når andre har ansvar og det ikke finnes noen bekreftelser skal det ikke genereres nye bekreftelser når " - {
        listOf(
            1.seconds,
            1.days,
            10.days,
            20.days,
            50.days
        ).forEach { tidEtterStart ->
            "bekreftelse puntuator kjører $tidEtterStart etter start av periode" - {
                val resultat = sequenceOf(internTilstand to ansvar)
                    .prosessererBekreftelser(
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
    "Når andre har ansvar og det finnes en ikke besvart bekreftelse skal det ikke skje noe " - {
        listOf(
            1.seconds,
            1.days,
            10.days,
            20.days,
            50.days
        ).forEach { tidEtterStart ->
            "bekreftelse puntuator kjører $tidEtterStart etter start av periode" - {
                val resultat = sequenceOf(
                    internTilstand.copy(
                        bekreftelser = listOf(
                            intervaller.bekreftelse(
                                gjelderFra = periodeStart,
                                gracePeriodeUtloept = null,
                                gracePeriodeVarselet = null
                            )
                        )
                    ) to ansvar
                )
                    .prosessererBekreftelser(
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