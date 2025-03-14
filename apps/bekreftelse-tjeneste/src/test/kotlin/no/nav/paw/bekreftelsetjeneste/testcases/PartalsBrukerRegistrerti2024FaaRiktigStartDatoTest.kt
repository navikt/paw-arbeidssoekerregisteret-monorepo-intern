package no.nav.paw.bekreftelsetjeneste.testcases

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssoekerregisteret.testdata.KafkaKeyContext
import no.nav.paw.arbeidssoekerregisteret.testdata.ValueWithKafkaKeyData
import no.nav.paw.arbeidssoekerregisteret.testdata.bekreftelse.bekreftelseMelding
import no.nav.paw.arbeidssoekerregisteret.testdata.mainavro.metadata
import no.nav.paw.arbeidssoekerregisteret.testdata.mainavro.periode
import no.nav.paw.bekreftelse.internehendelser.BekreftelseMeldingMottatt
import no.nav.paw.bekreftelse.internehendelser.BekreftelseTilgjengelig
import no.nav.paw.bekreftelse.internehendelser.PeriodeAvsluttet
import no.nav.paw.bekreftelsetjeneste.testutils.dager
import no.nav.paw.bekreftelsetjeneste.testutils.run
import no.nav.paw.bekreftelsetjeneste.testutils.setOppTest
import no.nav.paw.bekreftelsetjeneste.testutils.timestamp
import no.nav.paw.bekreftelsetjeneste.topology.DummyOddetallPartallMap
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class PartalsBrukerRegistrerti2024FaaRiktigStartDatoTest : FreeSpec({
    val identietsnummer = "12345678902"
    val periodeStartet = "02.02.2024 12:06".timestamp //Første mandag i februar 2025
    val interval = 14.dager
    val graceperiode = 7.dager
    val tilgjengeligOffset = 3.dager
    with(
        setOppTest(
            datoOgKlokkeslettVedStart = "01.12.2024 00:00".timestamp,
            tidlistBekreftelsePeriodeStart = LocalDate.parse("2025-03-10"),
            bekreftelseIntervall = interval,
            tilgjengeligOffset = tilgjengeligOffset,
            innleveringsfrist = graceperiode,
            oddetallPartallMap = DummyOddetallPartallMap()
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
                "02.02.2024 12:06".timestamp to periode,
                "29.03.2025 13:34".timestamp to ValueWithKafkaKeyData(
                    periode.id, periode.key, bekreftelseMelding(
                        periodeId = periode.value.id,
                        gjelderFra = "17.03.2025 00:00".timestamp,
                        gjelderTil = "31.03.2025 00:00".timestamp,
                        harJobbetIDennePerioden = true,
                        vilFortsetteSomArbeidssoeker = true
                    )
                ),
                "01.04.2025 03:12".timestamp to periode(
                    periodeId = periode.value.id,
                    identitetsnummer = periode.value.identitetsnummer,
                    startetMetadata = periode.value.startet,
                    avsluttetMetadata = metadata(tidspunkt = "01.04.2025 03:11".timestamp)
                )
            )
            run(eksterneHendelser, stoppTid, opploesning) to eksterneHendelser
        }
        val kilde = mutableListOf(*hendelser.toTypedArray())
        val input = eksterneHendelser.toMutableList()
        forventer<BekreftelseTilgjengelig>(
            kilde,
            input,
            fra = "27.03.2025 23:00".timestamp,
            til = "28.03.2025 06:00".timestamp,
            asserts = {
                it.size shouldBe 1
                it.first().gjelderFra shouldBe "17.03.2025 00:00".timestamp
                it.first().gjelderTil shouldBe "31.03.2025 00:00".timestamp
            }
        )
        forventer<BekreftelseMeldingMottatt>(
            kilde,
            input,
            fra = "29.03.2025 13:34".timestamp,
            til = "29.03.2025 13:40".timestamp
        )
        forventer<PeriodeAvsluttet>(
            kilde,
            input,
            fra = "01.04.2025 03:12".timestamp,
            til = "01.04.2025 03:18".timestamp
        )
        "Ingen flere hendelser inntraff" {
            kilde.shouldBeEmpty()
        }
    }
})
