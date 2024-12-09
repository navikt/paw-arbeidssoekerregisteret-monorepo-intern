package no.nav.paw.arbeidssokerregisteret.app.tilstand

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssokerregisteret.app.config.ApplicationLogicConfig
import no.nav.paw.arbeidssokerregisteret.app.dbNavn
import no.nav.paw.arbeidssokerregisteret.app.eventlogTopicNavn
import no.nav.paw.arbeidssokerregisteret.app.kafkaStreamProperties
import no.nav.paw.arbeidssokerregisteret.app.opplysningerOmArbeidssoekerTopicNavn
import no.nav.paw.arbeidssokerregisteret.app.opprettStreamsBuilder
import no.nav.paw.arbeidssokerregisteret.app.periodeTopicNavn
import no.nav.paw.arbeidssokerregisteret.app.topology
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.TopologyTestDriver
import java.time.Duration

class TilstandsoppryddingTest : StringSpec({
    val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val opplysningerTilPeriodeVindu = Duration.ofSeconds(60)
    val topology = topology(
        prometheusMeterRegistry = prometheusMeterRegistry,
        builder = opprettStreamsBuilder(dbNavn, Serdes.String()),
        dbNavn = dbNavn,
        innTopic = eventlogTopicNavn,
        periodeTopic = periodeTopicNavn,
        opplysningerOmArbeidssoekerTopic = opplysningerOmArbeidssoekerTopicNavn,
        applicationLogicConfig = ApplicationLogicConfig(
            inkluderOpplysningerInnenforTidsvindu = opplysningerTilPeriodeVindu
        )
    )

    val testDriver = TopologyTestDriver(topology, kafkaStreamProperties)
    val stringStateStore = testDriver.getKeyValueStore<Long, String?>(dbNavn)

    "Om TilstandV1 er 'null' eller null skal den fjernes av Tilstandsopprydding" {
        stringStateStore.put(1234L, "null")
        testDriver.advanceWallClockTime(Duration.ofMinutes(30))
        val currentState = stringStateStore.get(1234L)
        currentState shouldBe null
    }
    "Om TilstandV1 ikke er 'null' eller null skal den ikke fjernes av Tilstandsopprydding" {
        stringStateStore.put(1234L, "ikke null")
        testDriver.advanceWallClockTime(Duration.ofMinutes(30))
        val currentState = stringStateStore.get(1234L)
        currentState shouldBe "ikke null"
    }
})