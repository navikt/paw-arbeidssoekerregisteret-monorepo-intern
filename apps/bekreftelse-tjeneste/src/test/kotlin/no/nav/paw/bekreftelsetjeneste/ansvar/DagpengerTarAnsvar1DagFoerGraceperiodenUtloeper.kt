package no.nav.paw.bekreftelsetjeneste.ansvar

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.paw.bekreftelse.ansvar.v1.vo.TarAnsvar
import no.nav.paw.bekreftelse.internehendelser.AndreHarOvertattAnsvar
import no.nav.paw.bekreftelsetjeneste.*
import no.nav.paw.bekreftelsetjeneste.config.BekreftelseIntervals
import no.nav.paw.bekreftelsetjeneste.tilstand.*
import java.time.Duration
import java.time.Instant

class DagpengerTarAnsvar1DagFoerGraceperiodenUtloeper : FreeSpec({
    val intervaller = BekreftelseIntervals(
        interval = 14.days,
        tilgjengeligOffset = 3.days,
        graceperiode = 7.days,
        varselFoerGraceperiodeUtloept = 3.days
    )

    val periodeStart = Instant.parse("2024-10-23T08:00:00Z")
    val initiellTilstand = internTilstand(periodeStart)

    "${Loesning.DAGPENGER} tar ansvar 1 dag før grace perioden utøper" - {
        val tilstand = initiellTilstand.leggTilNyEllerOppdaterBekreftelse(
            ny = intervaller.bekreftelse(
                gjelderFra = periodeStart,
                gracePeriodeUtloept = null
            )
        )
        val dagpengerTarAnsvar = no.nav.paw.arbeidssoekerregisteret.testdata.bekreftelse.tarAnsvar(
            periodeId = tilstand.periode.periodeId,
            bekreftelsesloesning = no.nav.paw.bekreftelse.ansvar.v1.vo.Bekreftelsesloesning.DAGPENGER,
        )
        val handlinger = haandterAnsvarEndret(
            wallclock = WallClock(intervaller.gracePeriodeUtloeper(periodeStart) - 1.days),
            tilstand = tilstand,
            ansvar = null,
            ansvarEndret = dagpengerTarAnsvar
        )
        "Ansvar skal skrives til key-value store" {
            handlinger.assertExactlyOne<Handling, SkrivAnsvar> {
                id shouldBe tilstand.periode.periodeId
                value shouldBe Ansvar(
                    periodeId = tilstand.periode.periodeId,
                    ansvarlige = listOf(
                        Ansvarlig(
                            loesning = Loesning.DAGPENGER,
                            intervall = Duration.ofMillis((dagpengerTarAnsvar.handling as TarAnsvar).intervalMS),
                            gracePeriode = Duration.ofMillis((dagpengerTarAnsvar.handling as TarAnsvar).graceMS)
                        )
                    )
                )
            }
        }
        "AndreHarOvertattAnsvar hendelse skal sendes" {
            handlinger.assertExactlyOne<Handling, SendHendelse> {
                hendelse.shouldBeInstanceOf<AndreHarOvertattAnsvar>()
                hendelse.periodeId shouldBe tilstand.periode.periodeId
            }
        }
        "Åpne bekreftelser settes til AnsvarOvertattAvAndre" {
            withClue("Handlinger inneholdt ikke skriving av intern tilstand med forventet status: ${handlinger.filterIsInstance<SkrivInternTilstand>()}") {
                handlinger.assertExactlyOne<Handling, SkrivInternTilstand> {
                    id shouldBe tilstand.periode.periodeId
                    value.bekreftelser.size shouldBe 1
                    value.bekreftelser.first().sisteTilstand().shouldBeInstanceOf<AnsvarOvertattAvAndre>()
                }
            }
        }
        "Ingen andre handlinger skal utføres" {
            handlinger.size shouldBe 3
        }
    }
})
