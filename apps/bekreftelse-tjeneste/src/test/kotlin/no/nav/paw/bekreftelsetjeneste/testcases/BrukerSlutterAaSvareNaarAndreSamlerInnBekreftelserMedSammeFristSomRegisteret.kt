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
import no.nav.paw.bekreftelse.internehendelser.BekreftelseMeldingMottatt
import no.nav.paw.bekreftelse.internehendelser.BekreftelsePaaVegneAvStartet
import no.nav.paw.bekreftelse.internehendelser.BekreftelseTilgjengelig
import no.nav.paw.bekreftelse.internehendelser.LeveringsfristUtloept
import no.nav.paw.bekreftelse.internehendelser.PeriodeAvsluttet
import no.nav.paw.bekreftelse.internehendelser.RegisterGracePeriodeGjenstaaendeTid
import no.nav.paw.bekreftelse.internehendelser.RegisterGracePeriodeUtloeptEtterEksternInnsamling
import no.nav.paw.bekreftelse.melding.v1.vo.Bekreftelsesloesning
import no.nav.paw.bekreftelsetjeneste.testutils.dager
import no.nav.paw.bekreftelsetjeneste.testutils.run
import no.nav.paw.bekreftelsetjeneste.testutils.setOppTest
import no.nav.paw.bekreftelsetjeneste.testutils.timestamp
import java.time.Duration
import java.time.Instant

class BrukerSlutterAaSvareNaarAndreSamlerInnBekreftelserMedSammeFristSomRegisteret : FreeSpec({
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
        val opploesning = Duration.ofSeconds(180)
        val stoppTid = "02.05.2025 00:00".timestamp
        val (hendelser, eksterneHendelser) = with(KafkaKeyContext(this.kafkaKeysClient)) {
            val periode = periode(
                identitetsnummer = identietsnummer,
                startetMetadata = metadata(tidspunkt = periodeStartet)
            )
            val eksterneHendelser: List<Pair<Instant, ValueWithKafkaKeyData<*>>> = listOf(
                "05.02.2025 15:26".timestamp to periode,
                "15.02.2025 13:34".timestamp to ValueWithKafkaKeyData(
                    periode.id, periode.key, bekreftelseMelding(
                        periodeId = periode.value.id,
                        gjelderFra = periodeStartet,
                        gjelderTil = "24.02.2025 00:00".timestamp,
                        harJobbetIDennePerioden = true,
                        vilFortsetteSomArbeidssoeker = true
                    )
                ),
                "07.03.2025 09:12".timestamp to ValueWithKafkaKeyData(
                    periode.id, periode.key, startPaaVegneAv(
                        periodeId = periode.value.id,
                        bekreftelsesloesning = no.nav.paw.bekreftelse.paavegneav.v1.vo.Bekreftelsesloesning.DAGPENGER,
                        grace = graceperiode,
                        interval = interval
                    )
                ),
                "17.03.2025 12:01".timestamp to ValueWithKafkaKeyData(
                    periode.id, periode.key, bekreftelseMelding(
                        periodeId = periode.value.id,
                        gjelderFra = "03.03.2025 00:00".timestamp,
                        gjelderTil = "17.03.2025 00:00".timestamp,
                        harJobbetIDennePerioden = true,
                        vilFortsetteSomArbeidssoeker = true,
                        bekreftelsesloesning = Bekreftelsesloesning.DAGPENGER
                    )
                ),
                "08.04.2025 05:00".timestamp to ValueWithKafkaKeyData(
                    periode.id, periode.key, stoppPaaVegneAv(
                        periodeId = periode.value.id,
                        bekreftelsesloesning = no.nav.paw.bekreftelse.paavegneav.v1.vo.Bekreftelsesloesning.DAGPENGER,
                        fristBrutt = true
                    )
                )
            )
            run(eksterneHendelser, stoppTid, opploesning) to eksterneHendelser
        }
        val kilde = mutableListOf(*hendelser.toTypedArray())
        val input = eksterneHendelser.toMutableList()
        forventer<BekreftelseTilgjengelig>(
            kilde,
            input,
            fra = "14.02.2025 00:00".timestamp,
            til = "14.02.2025 06:00".timestamp
        )
        forventer<BekreftelseMeldingMottatt>(
            kilde,
            input,
            fra = "15.02.2025 13:34".timestamp,
            til = "15.02.2025 13:40".timestamp
        )
        forventer<BekreftelseTilgjengelig>(
            kilde,
            input,
            fra = "28.02.2025 00:00".timestamp,
            til = "28.02.2025 06:00".timestamp
        )
        forventer<LeveringsfristUtloept>(
            kilde,
            input,
            fra = "03.03.2025 00:00".timestamp,
            til = "03.03.2025 03:00".timestamp
        )
        forventer<RegisterGracePeriodeGjenstaaendeTid>(
            kilde,
            input,
            fra = "06.03.2025 12:00".timestamp,
            til = "06.03.2025 12:15".timestamp
        )
        forventer<BekreftelsePaaVegneAvStartet>(
            kilde,
            input,
            fra = "07.03.2025 09:12".timestamp,
            til = "07.03.2025 09:18".timestamp
        )
        forventer<RegisterGracePeriodeUtloeptEtterEksternInnsamling>(
            kilde,
            input,
            fra = "08.04.2025 05:00".timestamp,
            til = "08.04.2025 05:06".timestamp
        )
        forventer<PeriodeAvsluttet>(
            kilde,
            input,
            fra = "08.04.2025 05:00".timestamp,
            til = "08.04.2025 05:06".timestamp
        )
        "Ingen flere hendelser inntraff" {
            kilde.shouldBeEmpty()
        }
    }
})

