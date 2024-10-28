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

class DagpengerTarAnsvar1DagFoerGraceperiodenUtloeper : FreeSpec({
    val intervaller = standardIntervaller

    val periodeStart = Instant.parse("2024-10-23T08:00:00Z")
    val initiellTilstand = internTilstand(periodeStart = periodeStart)

    "${Loesning.DAGPENGER} tar ansvar 1 dag før grace perioden utøper" - {
        val tilstand = initiellTilstand.leggTilNyEllerOppdaterBekreftelse(
            intervaller.bekreftelse(
                gjelderFra = periodeStart,
                gracePeriodeUtloept = null
            )
        )
        val dagpengerTarAnsvar = no.nav.paw.arbeidssoekerregisteret.testdata.bekreftelse.startPaaVegneAv(
            periodeId = tilstand.periode.periodeId,
            bekreftelsesloesning = no.nav.paw.bekreftelse.paavegneav.v1.vo.Bekreftelsesloesning.DAGPENGER,
        )
        val handlinger = haandterBekreftelsePaaVegneAvEndret(
            wallclock = WallClock(intervaller.gracePeriodeUtloeper(periodeStart) - 1.days),
            tilstand = tilstand,
            paaVegneAvTilstand = null,
            paaVegneAv = dagpengerTarAnsvar
        )
        "Ansvar skal skrives til key-value store" {
            handlinger.assertExactlyOne<Handling, SkrivBekreftelsePaaVegneAv> {
                id shouldBe tilstand.periode.periodeId
                value shouldBe PaaVegneAvTilstand(
                    periodeId = tilstand.periode.periodeId,
                    internPaaVegneAvList = listOf(
                        InternPaaVegneAv(
                            loesning = Loesning.DAGPENGER,
                            intervall = Duration.ofMillis((dagpengerTarAnsvar.handling as Start).intervalMS),
                            gracePeriode = Duration.ofMillis((dagpengerTarAnsvar.handling as Start).graceMS)
                        )
                    )
                )
            }
        }
        "AndreHarOvertattAnsvar hendelse skal sendes" {
            handlinger.assertExactlyOne<Handling, SendHendelse> {
                hendelse.shouldBeInstanceOf<BekreftelsePaaVegneAvStartet>()
                hendelse.periodeId shouldBe tilstand.periode.periodeId
            }
        }
        "Åpne bekreftelser settes til AnsvarOvertattAvAndre" {
            withClue("Handlinger inneholdt ikke skriving av intern tilstand med forventet status: ${handlinger.filterIsInstance<SkrivInternTilstand>()}") {
                handlinger.assertExactlyOne<Handling, SkrivInternTilstand> {
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
