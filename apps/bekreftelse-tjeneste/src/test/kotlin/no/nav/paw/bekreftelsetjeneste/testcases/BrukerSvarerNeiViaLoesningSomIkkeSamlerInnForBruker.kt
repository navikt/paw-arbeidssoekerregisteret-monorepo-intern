package no.nav.paw.bekreftelsetjeneste.testcases

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import no.nav.paw.arbeidssoekerregisteret.testdata.KafkaKeyContext
import no.nav.paw.arbeidssoekerregisteret.testdata.ValueWithKafkaKeyData
import no.nav.paw.arbeidssoekerregisteret.testdata.bekreftelse.bekreftelseMelding
import no.nav.paw.arbeidssoekerregisteret.testdata.bekreftelse.startPaaVegneAv
import no.nav.paw.arbeidssoekerregisteret.testdata.mainavro.metadata
import no.nav.paw.arbeidssoekerregisteret.testdata.mainavro.periode
import no.nav.paw.bekreftelse.internehendelser.BaOmAaAvsluttePeriode
import no.nav.paw.bekreftelse.internehendelser.BekreftelsePaaVegneAvStartet
import no.nav.paw.bekreftelse.internehendelser.PeriodeAvsluttet
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
        val opploesning = Duration.ofSeconds(180)
        val stoppTid = "02.05.2025 00:00".timestamp
        val (hendelser, eksterneHendelser) = with(KafkaKeyContext(this.kafkaKeysClient)) {
            val periodeStartetTimestamp = "07.02.2025 15:26".timestamp //Første fredag i februar 2025
            val periode = periode(
                identitetsnummer = identietsnummer,
                startetMetadata = metadata(tidspunkt = periodeStartetTimestamp)
            )
            val eksterneHendelser: List<Pair<Instant, ValueWithKafkaKeyData<*>>> = listOf(
                periodeStartetTimestamp to periode,
                (periodeStartetTimestamp + 1.timer)  to ValueWithKafkaKeyData(
                    periode.id, periode.key, startPaaVegneAv(
                        periodeId = periode.value.id,
                        bekreftelsesloesning = no.nav.paw.bekreftelse.paavegneav.v1.vo.Bekreftelsesloesning.FRISKMELDT_TIL_ARBEIDSFORMIDLING,
                        grace = Duration.ofDays(7),
                        interval = Duration.ofDays(14)
                    )
                ),
                "24.02.2025 12:01".timestamp to ValueWithKafkaKeyData(
                    periode.id, periode.key, bekreftelseMelding(
                        periodeId = periode.value.id,
                        gjelderFra = "03.02.2025 00:00".timestamp,
                        gjelderTil = "17.02.2025 00:00".timestamp,
                        harJobbetIDennePerioden = true,
                        vilFortsetteSomArbeidssoeker = false,
                        bekreftelsesloesning = Bekreftelsesloesning.DAGPENGER
                    )
                )
            )
            run(eksterneHendelser, stoppTid, opploesning) to eksterneHendelser
        }
        val kilde = mutableListOf(*hendelser.toTypedArray())
        val input = eksterneHendelser.toMutableList()
        forventer<BekreftelsePaaVegneAvStartet>(
            kilde,
            input,
            fra = "07.02.2025 16:26".timestamp,
            til = "07.02.2025 16:30".timestamp
        )
        "Ingen flere hendelser inntraff" {
            kilde.shouldBeEmpty()
        }
    }
})
