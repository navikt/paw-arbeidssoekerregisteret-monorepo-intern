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
import no.nav.paw.bekreftelsetjeneste.testutils.timestamp
import no.nav.paw.bekreftelsetjeneste.tilstand.GracePeriodeVarselet
import java.time.Duration
import java.time.Instant
import java.util.*

class BrukerKommerTilbakeEnDagFoerFristUtenAaHaLevertNoe : FreeSpec({
    val identietsnummer = "12345678901"
    val periodeStartet = "03.03.2025 15:26".timestamp //Første onsdag i februar 2025
    val interval = 14.dager
    val graceperiode = 7.dager
    val tilgjengeligOffset = 3.dager
    with(
        setOppTest(
            datoOgKlokkeslettVedStart = periodeStartet,
            bekreftelseIntervall = interval,
            tilgjengeligOffset = tilgjengeligOffset,
            innleveringsfrist = graceperiode
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
                "03.03.2025 15:26".timestamp to periode,
                "15.03.2025 09:12".timestamp to ValueWithKafkaKeyData(periode.id, periode.key, startPaaVegneAv(
                    periodeId = periode.value.id,
                    bekreftelsesloesning = no.nav.paw.bekreftelse.paavegneav.v1.vo.Bekreftelsesloesning.DAGPENGER,
                    grace = 7.dager,
                    interval = 14.dager
                )),
                "23.03.2025 12:30".timestamp to ValueWithKafkaKeyData(periode.id, periode.key, stoppPaaVegneAv(
                    periodeId = periode.value.id,
                    bekreftelsesloesning = no.nav.paw.bekreftelse.paavegneav.v1.vo.Bekreftelsesloesning.DAGPENGER
                )),
                "06.04.2025 19:01".timestamp to ValueWithKafkaKeyData(periode.id, periode.key, bekreftelseMelding(
                    periodeId = periode.value.id,
                    harJobbetIDennePerioden = true,
                    vilFortsetteSomArbeidssoeker = true,
                    bekreftelsesloesning = Bekreftelsesloesning.ARBEIDSSOEKERREGISTERET
                ))
            )
            run(eksterneHendelser, stoppTid, opploesning) to eksterneHendelser
        }
        hendelser.map { (ts, hendelse) -> "${ts.prettyPrint}: ${hendelse.prettyPrint()}" }.forEach { println(it) }
        val kilde = mutableListOf(*hendelser.toTypedArray())
        val inputHendelser = input.toMutableList()
        forventer<BekreftelseTilgjengelig>(
            kilde,
            inputHendelser,
            fra = "14.03.2025 00:00".timestamp,
            til = "14.03.2025 06:00".timestamp,
            asserts = { publiserteBekreftelser ->
                publiserteBekreftelser.size shouldBe 1
                publiserteBekreftelser.first() should { bekreftelse ->
                    bekreftelse.gjelderFra shouldBe "03.03.2025 15:26".timestamp
                    bekreftelse.gjelderTil shouldBe "17.03.2025 00:00".timestamp
                }
            }
        )
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
            fra = "27.03.2025 23:00".timestamp,
            til = "28.03.2025 13:00".timestamp,
            asserts = { publiserteBekreftelser ->
                publiserteBekreftelser.size shouldBe 1
                publiserteBekreftelser.first() should { bekreftelse ->
                    bekreftelse.gjelderFra shouldBe "17.03.2025 00:00".timestamp
                    bekreftelse.gjelderTil shouldBe "31.03.2025 00:00".timestamp
                }
                bekreftelseId2803 = publiserteBekreftelser.first().bekreftelseId
            }
        )
        forventer<LeveringsfristUtloept>(
            kilde,
            inputHendelser,
            fra = "31.03.2025 00:00".timestamp,
            til = "31.03.2025 00:20".timestamp,
            asserts = { fristUtloeptListe ->
                fristUtloeptListe.size shouldBe 1
                fristUtloeptListe.first() should { fristUtloept ->
                    fristUtloept.leveringsfrist shouldBe "31.03.2025 00:00".timestamp
                    fristUtloept.bekreftelseId shouldBe bekreftelseId2803
                }
            }
        )
        forventer<RegisterGracePeriodeGjenstaaendeTid>(
            kilde,
            inputHendelser,
            fra = "03.04.2025 12:00".timestamp,
            til = "03.04.2025 12:30".timestamp,
        )
        forventer<BekreftelseMeldingMottatt>(
            kilde,
            inputHendelser,
            fra = "06.04.2025 19:01".timestamp,
            til = "06.04.2025 19:07".timestamp,
            asserts = { publiserteBekreftelser ->
                publiserteBekreftelser.size shouldBe 1
                publiserteBekreftelser.first() should { bekreftelse ->
                    bekreftelse.bekreftelseId shouldBe bekreftelseId2803
                }
            }
        )
        var bekreftelseId1104: UUID? = null
        forventer<BekreftelseTilgjengelig>(
            kilde,
            inputHendelser,
            fra = "10.04.2025 23:00".timestamp,
            til = "11.04.2025 06:00".timestamp,
            asserts = { publiserteBekreftelser ->
                publiserteBekreftelser.size shouldBe 1
                publiserteBekreftelser.first() should { bekreftelse ->
                    bekreftelse.gjelderFra shouldBe "31.03.2025 00:00".timestamp
                    bekreftelse.gjelderTil shouldBe "14.04.2025 00:00".timestamp
                }
                bekreftelseId1104 = publiserteBekreftelser.first().bekreftelseId
            }
        )
        forventer<LeveringsfristUtloept>(
            kilde,
            inputHendelser,
            fra = "14.04.2025 00:00".timestamp,
            til = "14.04.2025 00:09".timestamp,
            asserts = { fristUtloeptListe ->
                fristUtloeptListe.size shouldBe 1
                fristUtloeptListe.first() should { fristUtloept ->
                    fristUtloept.leveringsfrist shouldBe "14.04.2025 00:00".timestamp
                    fristUtloept.bekreftelseId shouldBe bekreftelseId1104
                }
            }
        )
        forventer<RegisterGracePeriodeGjenstaaendeTid>(
            kilde,
            inputHendelser,
            fra = "17.04.2025 12:00".timestamp,
            til = "17.04.2025 12:20".timestamp,
            asserts = { gracePeriodeGjenstaaendeTidListe ->
                gracePeriodeGjenstaaendeTidListe.size shouldBe 1
                gracePeriodeGjenstaaendeTidListe.first() should { gracePeriodeGjenstaaendeTid ->
                    gracePeriodeGjenstaaendeTid.bekreftelseId shouldBe bekreftelseId1104
                }
            }
        )
        forventer<RegisterGracePeriodeUtloept>(
            kilde,
            inputHendelser,
            fra = "21.04.2025 00:00".timestamp,
            til = "21.04.2025 00:09".timestamp,
            asserts = { gracePeriodeUtloeptListe ->
                gracePeriodeUtloeptListe.size shouldBe 1
                gracePeriodeUtloeptListe.first() should { gracePeriodeUtloept ->
                    gracePeriodeUtloept.bekreftelseId shouldBe bekreftelseId1104
                }
            }
        )
        forventer<PeriodeAvsluttet>(
            kilde,
            inputHendelser,
            fra = "21.04.2025 00:00".timestamp,
            til = "21.04.2025 00:15".timestamp
        )
        "Ingen flere hendelser inntraff" {
            kilde.shouldBeEmpty()
        }
    }
})

