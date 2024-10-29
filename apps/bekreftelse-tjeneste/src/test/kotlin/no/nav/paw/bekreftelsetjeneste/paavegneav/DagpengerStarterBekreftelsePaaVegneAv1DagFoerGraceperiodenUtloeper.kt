package no.nav.paw.bekreftelsetjeneste.paavegneav

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.paw.bekreftelse.internehendelser.BekreftelsePaaVegneAvStartet
import no.nav.paw.bekreftelse.paavegneav.v1.vo.Start
import no.nav.paw.bekreftelsetjeneste.*
import no.nav.paw.bekreftelsetjeneste.tilstand.leggTilNyEllerOppdaterBekreftelse
import no.nav.paw.bekreftelsetjeneste.tilstand.sisteTilstand
import no.nav.paw.test.days
import java.time.Duration
import java.time.Instant

class DagpengerStarterBekreftelsePaaVegneAv1DagFoerGraceperiodenUtloeper : FreeSpec({
    val intervaller = standardIntervaller

    val periodeStart = Instant.parse("2024-10-23T08:00:00Z")
    val initiellTilstand = bekreftelseTilstand(periodeStart = periodeStart)

    "${Loesning.DAGPENGER} starter bekreftelsePaaVegneAv 1 dag før grace perioden utøper" - {
        val tilstand = initiellTilstand.leggTilNyEllerOppdaterBekreftelse(
            intervaller.bekreftelse(
                gjelderFra = periodeStart,
                gracePeriodeUtloept = null
            )
        )
        val dagpengerStarterBekreftelsePaaVegneAv = no.nav.paw.arbeidssoekerregisteret.testdata.bekreftelse.startPaaVegneAv(
            periodeId = tilstand.periode.periodeId,
            bekreftelsesloesning = no.nav.paw.bekreftelse.paavegneav.v1.vo.Bekreftelsesloesning.DAGPENGER,
        )
        val handlinger = haandterBekreftelsePaaVegneAvEndret(
            wallclock = WallClock(intervaller.gracePeriodeUtloeper(periodeStart) - 1.days),
            bekreftelseTilstand = tilstand,
            paaVegneAvTilstand = null,
            paaVegneAvHendelse = dagpengerStarterBekreftelsePaaVegneAv
        )
        "BekreftelsePaaVegneAv skal skrives til key-value store" {
            handlinger.assertExactlyOne<Handling, SkrivPaaVegneAvTilstand> {
                id shouldBe tilstand.periode.periodeId
                value shouldBe PaaVegneAvTilstand(
                    periodeId = tilstand.periode.periodeId,
                    paaVegneAvList = listOf(
                        InternPaaVegneAv(
                            loesning = Loesning.DAGPENGER,
                            intervall = Duration.ofMillis((dagpengerStarterBekreftelsePaaVegneAv.handling as Start).intervalMS),
                            gracePeriode = Duration.ofMillis((dagpengerStarterBekreftelsePaaVegneAv.handling as Start).graceMS)
                        )
                    )
                )
            }
        }
        "BekreftelsePaaVegneAvStartet hendelse skal sendes" {
            handlinger.assertExactlyOne<Handling, SendHendelse> {
                hendelse.shouldBeInstanceOf<BekreftelsePaaVegneAvStartet>()
                hendelse.periodeId shouldBe tilstand.periode.periodeId
            }
        }
        "Åpne bekreftelser settes til InternBekreftelsePaaVegneAvStartet" {
            withClue("Handlinger inneholdt ikke skriving av intern tilstand med forventet status: ${handlinger.filterIsInstance<SkrivBekreftelseTilstand>()}") {
                handlinger.assertExactlyOne<Handling, SkrivBekreftelseTilstand> {
                    id shouldBe tilstand.periode.periodeId
                    value.bekreftelser.size shouldBe 1
                    value.bekreftelser.first().sisteTilstand().shouldBeInstanceOf<no.nav.paw.bekreftelsetjeneste.tilstand.InternBekreftelsePaaVegneAvStartet>()
                }
            }
        }
        "Ingen andre handlinger skal utføres" {
            handlinger.size shouldBe 3
        }
    }
})
