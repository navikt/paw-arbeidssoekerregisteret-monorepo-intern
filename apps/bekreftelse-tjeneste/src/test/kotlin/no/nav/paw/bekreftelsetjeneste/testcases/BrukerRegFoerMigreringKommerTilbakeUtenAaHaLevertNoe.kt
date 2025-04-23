package no.nav.paw.bekreftelsetjeneste.testcases

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssoekerregisteret.testdata.KafkaKeyContext
import no.nav.paw.arbeidssoekerregisteret.testdata.ValueWithKafkaKeyData
import no.nav.paw.arbeidssoekerregisteret.testdata.bekreftelse.bekreftelseMelding
import no.nav.paw.arbeidssoekerregisteret.testdata.bekreftelse.startPaaVegneAv
import no.nav.paw.arbeidssoekerregisteret.testdata.bekreftelse.stoppPaaVegneAv
import no.nav.paw.arbeidssoekerregisteret.testdata.mainavro.metadata
import no.nav.paw.arbeidssoekerregisteret.testdata.mainavro.periode
import no.nav.paw.bekreftelse.internehendelser.BaOmAaAvsluttePeriode
import no.nav.paw.bekreftelse.internehendelser.BekreftelseMeldingMottatt
import no.nav.paw.bekreftelse.internehendelser.BekreftelsePaaVegneAvStartet
import no.nav.paw.bekreftelse.internehendelser.BekreftelseTilgjengelig
import no.nav.paw.bekreftelse.internehendelser.LeveringsfristUtloept
import no.nav.paw.bekreftelse.internehendelser.PeriodeAvsluttet
import no.nav.paw.bekreftelse.internehendelser.RegisterGracePeriodeGjenstaaendeTid
import no.nav.paw.bekreftelse.internehendelser.RegisterGracePeriodeUtloept
import no.nav.paw.bekreftelse.internehendelser.RegisterGracePeriodeUtloeptEtterEksternInnsamling
import no.nav.paw.bekreftelse.melding.v1.vo.Bekreftelsesloesning
import no.nav.paw.bekreftelsetjeneste.testutils.dager
import no.nav.paw.bekreftelsetjeneste.testutils.prettyPrint
import no.nav.paw.bekreftelsetjeneste.testutils.run
import no.nav.paw.bekreftelsetjeneste.testutils.setOppTest
import no.nav.paw.bekreftelsetjeneste.testutils.timer
import no.nav.paw.bekreftelsetjeneste.testutils.timestamp
import no.nav.paw.bekreftelsetjeneste.tilstand.GracePeriodeVarselet
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.util.*

class BrukerRegFoerMigreringKommerTilbakeUtenAaHaLevertNoe : FreeSpec({
    val identietsnummer = "12345678901"
    val periodeStartet = "01.01.2025 15:26".timestamp
    val interval = 14.dager
    val graceperiode = 7.dager
    val tilgjengeligOffset = 3.dager
    with(
        setOppTest(
            tidlistBekreftelsePeriodeStart = LocalDate.of(2025, Month.MARCH, 10),
            datoOgKlokkeslettVedStart = periodeStartet,
            bekreftelseIntervall = interval,
            tilgjengeligOffset = tilgjengeligOffset,
            innleveringsfrist = graceperiode + 4.timer
        )
    ) {
        "Setter opp test med ${interval.toDays()} dagers intervall og ${graceperiode.toDays()} dagers graceperiode. Bekreftelser tilgjengeliggjøres ${tilgjengeligOffset.toDays()} dager før utløp av ${interval.toDays()} dagers perioden" {}
        val opploesning = Duration.ofSeconds(180)
        val stoppTid = "02.05.2025 00:00".timestamp
        val (hendelser, input) = with(KafkaKeyContext(this.kafkaKeysClient)) {
            val periode = periode(
                identitetsnummer = identietsnummer,
                startetMetadata = metadata(tidspunkt = periodeStartet)
            )
            val eksterneHendelser: List<Pair<Instant, ValueWithKafkaKeyData<*>>> = listOf(
                "01.01.2025 15:26".timestamp to periode,
                "15.03.2025 09:12".timestamp to ValueWithKafkaKeyData(periode.id, periode.key, startPaaVegneAv(
                    periodeId = periode.value.id,
                    bekreftelsesloesning = no.nav.paw.bekreftelse.paavegneav.v1.vo.Bekreftelsesloesning.DAGPENGER,
                    grace = 7.dager,
                    interval = 14.dager
                )),
                "15.03.2025 12:30".timestamp to ValueWithKafkaKeyData(periode.id, periode.key, stoppPaaVegneAv(
                    periodeId = periode.value.id,
                    bekreftelsesloesning = no.nav.paw.bekreftelse.paavegneav.v1.vo.Bekreftelsesloesning.DAGPENGER
                ))
            )
            run(eksterneHendelser, stoppTid, opploesning) to eksterneHendelser
        }
        hendelser.map { (ts, hendelse) -> "${ts.prettyPrint}: ${hendelse.prettyPrint()}" }.forEach { println(it) }
        val kilde = mutableListOf(*hendelser.toTypedArray())
        val inputHendelser = input.toMutableList()
        forventer<BekreftelsePaaVegneAvStartet>(
            kilde,
            inputHendelser,
            fra = "15.03.2025 09:12".timestamp,
            til = "15.03.2025 09:18".timestamp
        )
        var bekreftelseId2803: UUID? = null
        forventer<BekreftelseTilgjengelig>(
            kilde,
            inputHendelser,
            fra = "21.03.2025 00:00".timestamp,
            til = "21.03.2025 06:00".timestamp,
            asserts = { publiserteBekreftelser ->
                publiserteBekreftelser.size shouldBe 1
                publiserteBekreftelser.first() should { bekreftelse ->
                    bekreftelse.gjelderFra shouldBe "10.03.2025 00:00".timestamp
                    bekreftelse.gjelderTil shouldBe "24.03.2025 00:00".timestamp
                }
                bekreftelseId2803 = publiserteBekreftelser.first().bekreftelseId
            }
        )
        forventer<LeveringsfristUtloept>(
            kilde,
            inputHendelser,
            fra = "24.03.2025 00:00".timestamp,
            til = "24.03.2025 00:10".timestamp,
            asserts = { fristUtloeptListe ->
                fristUtloeptListe.size shouldBe 1
                fristUtloeptListe.first() should { fristUtloept ->
                    fristUtloept.leveringsfrist shouldBe "24.03.2025 00:00".timestamp
                    fristUtloept.bekreftelseId shouldBe bekreftelseId2803
                }
            }
        )
        forventer<RegisterGracePeriodeGjenstaaendeTid>(
            kilde,
            inputHendelser,
            fra = "27.03.2025 00:30".timestamp,
            til = "27.03.2025 23:00".timestamp,
        )
        forventer<RegisterGracePeriodeUtloept>(
            kilde,
            inputHendelser,
            fra = "31.03.2025 00:00".timestamp,
            til = "31.03.2025 06:00".timestamp
        )
        forventer<PeriodeAvsluttet>(
            kilde,
            inputHendelser,
            fra = "31.03.2025 00:00".timestamp,
            til = "31.03.2025 06:00".timestamp
        )
        "Ingen flere hendelser inntraff" {
            kilde.shouldBeEmpty()
        }
    }
})

