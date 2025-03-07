package no.nav.paw.bekreftelsetjeneste.testcases

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssoekerregisteret.testdata.KafkaKeyContext
import no.nav.paw.arbeidssoekerregisteret.testdata.ValueWithKafkaKeyData
import no.nav.paw.arbeidssoekerregisteret.testdata.bekreftelse.bekreftelseMelding
import no.nav.paw.arbeidssoekerregisteret.testdata.bekreftelse.startPaaVegneAv
import no.nav.paw.arbeidssoekerregisteret.testdata.mainavro.metadata
import no.nav.paw.arbeidssoekerregisteret.testdata.mainavro.periode
import no.nav.paw.bekreftelse.internehendelser.BaOmAaAvsluttePeriode
import no.nav.paw.bekreftelse.internehendelser.BekreftelseMeldingMottatt
import no.nav.paw.bekreftelse.internehendelser.BekreftelsePaaVegneAvStartet
import no.nav.paw.bekreftelse.internehendelser.BekreftelseTilgjengelig
import no.nav.paw.bekreftelse.internehendelser.LeveringsfristUtloept
import no.nav.paw.bekreftelse.internehendelser.PeriodeAvsluttet
import no.nav.paw.bekreftelse.internehendelser.RegisterGracePeriodeGjenstaaendeTid
import no.nav.paw.bekreftelse.melding.v1.vo.Bekreftelsesloesning
import no.nav.paw.bekreftelsetjeneste.testutils.dager
import no.nav.paw.bekreftelsetjeneste.testutils.run
import no.nav.paw.bekreftelsetjeneste.testutils.setOppTest
import no.nav.paw.bekreftelsetjeneste.testutils.timer
import no.nav.paw.bekreftelsetjeneste.testutils.timestamp
import java.time.Duration
import java.time.Instant

class BrukerSvarerNeiViaLoesningSomIkkeSamlerInnForBruker : FreeSpec({
    val identietsnummer = "12345678901"
    val testStartTid = "07.02.2025 15:26".timestamp //Første fredag i februar 2025
    val interval = 14.dager
    val graceperiode = 7.dager
    val tilgjengeligOffset = 3.dager
    with(
        setOppTest(
            datoOgKlokkeslettVedStart = testStartTid,
            bekreftelseIntervall = interval,
            tilgjengeligOffset = tilgjengeligOffset,
            innleveringsfrist = graceperiode
        )
    ) {
        val periodeStartetTimestamp = "03.03.2025 15:26".timestamp //Første fredag i februar 2025
        val opploesning = Duration.ofSeconds(180)
        val stoppTid = "02.04.2025 00:00".timestamp
        val (hendelser, eksterneHendelser) = with(KafkaKeyContext(this.kafkaKeysClient)) {
            val periode = periode(
                identitetsnummer = identietsnummer,
                startetMetadata = metadata(tidspunkt = periodeStartetTimestamp)
            )
            val eksterneHendelser: List<Pair<Instant, ValueWithKafkaKeyData<*>>> = listOf(
                periodeStartetTimestamp to periode,
                "22.03.2025 12:01".timestamp to ValueWithKafkaKeyData(
                    periode.id, periode.key, bekreftelseMelding(
                        periodeId = periode.value.id,
                        gjelderFra = "10.03.2025 00:00".timestamp,
                        gjelderTil = "24.03.2025 00:00".timestamp,
                        harJobbetIDennePerioden = true,
                        vilFortsetteSomArbeidssoeker = false,
                        bekreftelsesloesning = Bekreftelsesloesning.DAGPENGER
                    )
                ),
                "22.03.2025 18:01".timestamp to ValueWithKafkaKeyData(
                    periode.id, periode.key, bekreftelseMelding(
                        periodeId = periode.value.id,
                        gjelderFra = periodeStartetTimestamp,
                        gjelderTil = "17.03.2025 00:00".timestamp,
                        harJobbetIDennePerioden = true,
                        vilFortsetteSomArbeidssoeker = true,
                        bekreftelsesloesning = Bekreftelsesloesning.ARBEIDSSOEKERREGISTERET
                    )
                ),
                "23.03.2025 09:12".timestamp to ValueWithKafkaKeyData(
                    periode.id, periode.key, periode(
                        periodeId = periode.value.id,
                        startetMetadata = periode.value.startet,
                        avsluttetMetadata = metadata(tidspunkt = "23.03.2025 09:11".timestamp)
                    ).value
                )
            )
            run(eksterneHendelser, stoppTid, opploesning) to eksterneHendelser
        }
        val kilde = mutableListOf(*hendelser.toTypedArray())
        val input = eksterneHendelser.toMutableList()
        var til: Instant? = null
        forventer<BekreftelseTilgjengelig>(
            kilde,
            input,
            fra = "14.03.2025 00:00".timestamp,
            til = "14.03.2025 00:10".timestamp,
            asserts = { tilgjengelige ->
                tilgjengelige.size shouldBe 1
                tilgjengelige.first().gjelderTil shouldBe "17.03.2025 00:00".timestamp
                tilgjengelige.first().gjelderFra shouldBe periodeStartetTimestamp
                til = tilgjengelige.first().gjelderTil
            }
        )
        forventer<LeveringsfristUtloept>(
            kilde,
            input,
            fra = "17.03.2025 00:00".timestamp,
            til = "17.03.2025 00:10".timestamp
        )
        forventer<RegisterGracePeriodeGjenstaaendeTid>(
            kilde,
            input,
            fra = "20.03.2025 12:05".timestamp,
            til = "20.03.2025 12:15".timestamp
        )
        forventer<BekreftelseMeldingMottatt>(
            kilde,
            input,
            fra = "22.03.2025 18:01".timestamp,
            til = "22.03.2025 18:10".timestamp
        )
        forventer<PeriodeAvsluttet>(
            kilde,
            input,
            fra = "23.03.2025 09:12".timestamp,
            til = "23.03.2025 09:22".timestamp
        )
        "Ingen flere hendelser inntraff" {
            kilde.shouldBeEmpty()
        }
    }
})
