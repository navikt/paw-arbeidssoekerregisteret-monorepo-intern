package no.nav.paw.bekreftelsetjeneste.ansvar

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.paw.bekreftelse.ansvar.v1.vo.TarAnsvar
import no.nav.paw.bekreftelse.internehendelser.AndreHarOvertattAnsvar
import no.nav.paw.bekreftelsetjeneste.config.BekreftelseIntervals
import no.nav.paw.bekreftelsetjeneste.tilstand.*
import java.time.Duration
import java.time.Instant
import java.util.*

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
        val tilstand = initiellTilstand.leggTilNyBekreftelse(
            ny = Bekreftelse(
                tilstandsLogg = BekreftelseTilstandsLogg(
                    siste = GracePeriodeVarselet(intervaller.gracePeriodeVarsel(periodeStart)),
                    tidligere = listOf(
                        KlarForUtfylling(intervaller.tilgjengelig(periodeStart)),
                        VenterSvar(intervaller.frist(periodeStart))
                    )
                ),
                bekreftelseId = UUID.randomUUID(),
                gjelderFra = periodeStart,
                gjelderTil = periodeStart.plus(intervaller.interval)
            )
        )
        val dagpengerTarAnsvar = no.nav.paw.arbeidssoekerregisteret.testdata.bekreftelse.tarAnsvar(
            periodeId = tilstand.periode.periodeId,
            bekreftelsesloesning = no.nav.paw.bekreftelse.ansvar.v1.vo.Bekreftelsesloesning.DAGPENGER,
        )
        val handlinger = haandterAnsvarEndret(
            wallclock = WallClock(periodeStart.plus(intervaller.graceperiode).minus(1.days)),
            tilstand = tilstand,
            ansvar = null,
            ansvarEndret = dagpengerTarAnsvar
        )
        "Ansvar skal skrives til key-value store" {
            handlinger.handling<SkrivAnsvar> {
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
            handlinger.handling<SendHendelse> {
                hendelse.shouldBeInstanceOf<AndreHarOvertattAnsvar>()
                hendelse.periodeId shouldBe tilstand.periode.periodeId
            }
        }
        "Åpne bekreftelser settes til AnsvarOvertattAvAndre" {
            handlinger.handling<SkrivInternTilstand> {
                id shouldBe tilstand.periode.periodeId
                value.bekreftelser.size shouldBe 1
                value.bekreftelser.first().sisteTilstand().shouldBeInstanceOf<AnsvarOvertattAvAndre>()
            }
        }
        "Ingen andre handlinger skal utføres" {
            handlinger.size shouldBe 3
        }
    }

})

inline fun <reified T : Handling> List<Handling>.handling(f: T.() -> Unit = { }): T =
    filterIsInstance<T>()
        .let {
            withClue("Expected exactly one ${T::class.simpleName} but found ${it.size}") {
                it.size shouldBe 1
            }
            it.first()
        }

fun internTilstand(periodeStart: Instant) = InternTilstand(
    periode = PeriodeInfo(
        periodeId = UUID.randomUUID(),
        identitetsnummer = "12345678901",
        arbeidsoekerId = 1L,
        recordKey = 1L,
        startet = periodeStart,
        avsluttet = null
    ),
    bekreftelser = emptyList()
)

fun BekreftelseIntervals.gracePeriodeVarsel(startTid: Instant): Instant =
    startTid.plus(interval).plus(graceperiode).minus(varselFoerGraceperiodeUtloept)

fun BekreftelseIntervals.gracePeriodeUtloeper(startTid: Instant): Instant =
    startTid.plus(interval).plus(graceperiode)

fun BekreftelseIntervals.tilgjengelig(startTid: Instant): Instant = startTid.plus(interval).plus(tilgjengeligOffset)

fun BekreftelseIntervals.frist(startTid: Instant): Instant = startTid.plus(interval)