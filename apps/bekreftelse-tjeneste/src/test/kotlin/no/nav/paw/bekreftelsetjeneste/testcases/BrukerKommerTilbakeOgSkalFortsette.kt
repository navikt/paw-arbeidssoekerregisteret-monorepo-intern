package no.nav.paw.bekreftelsetjeneste.testcases

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldBeEmpty
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
import no.nav.paw.bekreftelse.internehendelser.RegisterGracePeriodeUtloeptEtterEksternInnsamling
import no.nav.paw.bekreftelse.melding.v1.vo.Bekreftelsesloesning
import no.nav.paw.bekreftelsetjeneste.testutils.dager
import no.nav.paw.bekreftelsetjeneste.testutils.run
import no.nav.paw.bekreftelsetjeneste.testutils.setOppTest
import no.nav.paw.bekreftelsetjeneste.testutils.timestamp
import java.time.Duration
import java.time.Instant

class BrukerKommerTilbakeOgSkalFortsette : FreeSpec({
    val identietsnummer = "12345678901"
    val periodeStartet = "05.02.2025 15:26".timestamp //Første onsdag i februar 2025
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
                "05.02.2025 15:26".timestamp to periode,
                "22.02.2025 13:34".timestamp to ValueWithKafkaKeyData(
                    periode.id, periode.key, bekreftelseMelding(
                        periodeId = periode.value.id,
                        gjelderFra = periodeStartet,
                        gjelderTil = "24.02.2025 00:00".timestamp,
                        harJobbetIDennePerioden = true,
                        vilFortsetteSomArbeidssoeker = true
                    )
                ),
                "12.03.2025 09:12".timestamp to ValueWithKafkaKeyData(
                    periode.id, periode.key, startPaaVegneAv(
                        periodeId = periode.value.id,
                        bekreftelsesloesning = no.nav.paw.bekreftelse.paavegneav.v1.vo.Bekreftelsesloesning.DAGPENGER,
                        grace = Duration.ofDays(7),
                        interval = Duration.ofDays(14)
                    )
                ),
                "29.03.2025 12:01".timestamp to ValueWithKafkaKeyData(
                    periode.id, periode.key, bekreftelseMelding(
                        periodeId = periode.value.id,
                        gjelderFra = "10.03.2025 00:00".timestamp,
                        gjelderTil = "31.03.2025 00:00".timestamp,
                        harJobbetIDennePerioden = true,
                        vilFortsetteSomArbeidssoeker = true,
                        bekreftelsesloesning = Bekreftelsesloesning.DAGPENGER
                    )
                ),
                "04.04.2025 05:00".timestamp to ValueWithKafkaKeyData(
                    periode.id, periode.key, stoppPaaVegneAv(
                        periodeId = periode.value.id,
                        bekreftelsesloesning = no.nav.paw.bekreftelse.paavegneav.v1.vo.Bekreftelsesloesning.DAGPENGER
                    )
                ),
                "12.04.2025 16:54".timestamp to ValueWithKafkaKeyData(
                    periode.id, periode.key, bekreftelseMelding(
                        periodeId = periode.value.id,
                        gjelderFra = "31.03.2025 00:00".timestamp,
                        gjelderTil = "14.04.2025 00:00".timestamp,
                        harJobbetIDennePerioden = true,
                        vilFortsetteSomArbeidssoeker = false,
                        bekreftelsesloesning = Bekreftelsesloesning.ARBEIDSSOEKERREGISTERET
                    )
                )
            )
            run(eksterneHendelser, stoppTid, opploesning) to eksterneHendelser
        }
        val kilde = mutableListOf(*hendelser.toTypedArray())
        val inputHendelser = input.toMutableList()
        forventer<BekreftelseTilgjengelig>(
            kilde,
            inputHendelser,
            fra = "21.02.2025 00:00".timestamp,
            til = "21.02.2025 06:00".timestamp
        )
        forventer<BekreftelseMeldingMottatt>(
            kilde,
            inputHendelser,
            fra = "22.02.2025 13:34".timestamp,
            til = "22.02.2025 13:40".timestamp
        )
        forventer<BekreftelseTilgjengelig>(
            kilde,
            inputHendelser,
            fra = "07.03.2025 00:00".timestamp,
            til = "07.03.2025 06:00".timestamp
        )
        forventer<LeveringsfristUtloept>(
            kilde,
            inputHendelser,
            fra = "10.03.2025 00:00".timestamp,
            til = "10.03.2025 03:00".timestamp
        )
        forventer<BekreftelsePaaVegneAvStartet>(
            kilde,
            inputHendelser,
            fra = "12.03.2025 09:12".timestamp,
            til = "12.03.2025 09:18".timestamp
        )
        forventer<BekreftelseTilgjengelig>(
            kilde,
            inputHendelser,
            fra = "11.04.2025 00:00".timestamp,
            til = "11.04.2025 03:00".timestamp
        )
        forventer<BekreftelseMeldingMottatt>(
            kilde,
            inputHendelser,
            fra = "12.04.2025 16:54".timestamp,
            til = "12.04.2025 17:00".timestamp
        )
        forventer<BaOmAaAvsluttePeriode>(
            kilde,
            inputHendelser,
            fra = "12.04.2025 16:54".timestamp,
            til = "12.04.2025 17:00".timestamp
        )
        forventer<PeriodeAvsluttet>(
            kilde,
            inputHendelser,
            fra = "12.04.2025 16:54".timestamp,
            til = "12.04.2025 17:10".timestamp
        )
        "Ingen flere hendelser inntraff" {
            kilde.shouldBeEmpty()
        }
    }
})

