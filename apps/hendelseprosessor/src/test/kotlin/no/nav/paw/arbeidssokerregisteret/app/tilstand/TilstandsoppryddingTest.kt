package no.nav.paw.arbeidssokerregisteret.app.tilstand

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssokerregisteret.app.config.ApplicationLogicConfig
import no.nav.paw.arbeidssokerregisteret.app.dbNavn
import no.nav.paw.arbeidssokerregisteret.app.eventlogTopicNavn
import no.nav.paw.arbeidssokerregisteret.app.funksjoner.HendelseScope
import no.nav.paw.arbeidssokerregisteret.app.funksjoner.kafkastreamsprocessors.skalSlettes
import no.nav.paw.arbeidssokerregisteret.app.kafkaStreamProperties
import no.nav.paw.arbeidssokerregisteret.app.opplysningerOmArbeidssoekerTopicNavn
import no.nav.paw.arbeidssokerregisteret.app.opprettStreamsBuilder
import no.nav.paw.arbeidssokerregisteret.app.periodeTopicNavn
import no.nav.paw.arbeidssokerregisteret.app.tilstandSerde
import no.nav.paw.arbeidssokerregisteret.app.topology
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Bruker
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.BrukerType
import org.apache.kafka.streams.TopologyTestDriver
import java.time.Duration
import java.time.Instant
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Metadata
import java.time.temporal.ChronoUnit
import java.util.*

class TilstandsoppryddingTest : StringSpec({
    val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val opplysningerTilPeriodeVindu = Duration.ofSeconds(60)
    val topology = topology(
        prometheusMeterRegistry = prometheusMeterRegistry,
        builder = opprettStreamsBuilder(dbNavn, tilstandSerde),
        dbNavn = dbNavn,
        innTopic = eventlogTopicNavn,
        periodeTopic = periodeTopicNavn,
        opplysningerOmArbeidssoekerTopic = opplysningerOmArbeidssoekerTopicNavn,
        applicationLogicConfig = ApplicationLogicConfig(
            inkluderOpplysningerInnenforTidsvindu = opplysningerTilPeriodeVindu
        )
    )

    val testDriver = TopologyTestDriver(topology, kafkaStreamProperties)
    val stateStore = testDriver.getKeyValueStore<Long, TilstandV1?>(dbNavn)

    "Om TilstandV1 inneholder avsluttet periode eldre enn 6 måneder skal den fjernes av Tilstandsopprydding" {
        val testTilstand = opprettTilstandV1(true)
        stateStore.put(1234L, testTilstand)
        testDriver.advanceWallClockTime(Duration.ofDays(1))
        val currentState = stateStore.get(1234L)
        currentState shouldBe null
    }
    "Om TilstandV1 ikke inneholder avsluttet periode eldre enn 6 måneder skal den ikke fjernes av Tilstandsopprydding" {
        val testTilstand = opprettTilstandV1(false)
        stateStore.put(1234L, testTilstand)
        testDriver.advanceWallClockTime(Duration.ofDays(1))
        val currentState = stateStore.get(1234L)
        currentState shouldBe testTilstand
    }
    "SkalSlettes() skal returnere true om avsluttet periode er eldre enn 6 måneder" {
        val testTilstand = opprettTilstandV1(true)
        testTilstand.skalSlettes() shouldBe true

        val testTilstand2: TilstandV1? = null
        testTilstand2.skalSlettes() shouldBe true
    }
    "SkalSlettes skal returnere false om periode ikke er avsluttet" {
        val testTilstand = opprettTilstandV1(false)
        testTilstand.skalSlettes() shouldBe false
    }
})

fun opprettTilstandV1(avsluttet: Boolean) =
    TilstandV1(
        hendelseScope = HendelseScope(
           key = 1234L,
              id = 1234L,
            partition = 0,
            offset = 0,
        ),
        gjeldeneTilstand = if (avsluttet) GjeldeneTilstand.AVSLUTTET else GjeldeneTilstand.STARTET,
        gjeldeneIdentitetsnummer = "12345678901",
        alleIdentitetsnummer = setOf("12345678901"),
        gjeldenePeriode = if (avsluttet) null else opprettTestPeriode(false),
        forrigePeriode = if (avsluttet) opprettTestPeriode(true) else null,
        sisteOpplysningerOmArbeidssoeker = null,
        forrigeOpplysningerOmArbeidssoeker = null
    )

fun opprettTestPeriode(avsluttet: Boolean) =
    Periode(
        id = UUID.randomUUID(),
        identitetsnummer = "12345678901",
        startet = opprettTestMetadata(Instant.now().minus(Duration.ofDays(179))),
        avsluttet = if (avsluttet) opprettTestMetadata(Instant.now().minus(Duration.ofDays(181))) else null
    )

fun opprettTestMetadata(tidspunkt: Instant) =
    Metadata(
        tidspunkt = tidspunkt.truncatedTo(ChronoUnit.MICROS),
        utfoertAv = Bruker(
            type = BrukerType.UDEFINERT,
            id = "12345678901",
        ),
        kilde = "test",
        aarsak = "test",
        tidspunktFraKilde = null
    )