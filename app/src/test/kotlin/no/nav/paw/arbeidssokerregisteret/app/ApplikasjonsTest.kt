package no.nav.paw.arbeidssokerregisteret.app

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssokerregisteret.intern.v1.*
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.TopologyTestDriver
import java.time.Instant
import java.util.*


class ApplikasjonsTest : StringSpec({
    "Verifiser at vi oppretter en ny periode ved førstegangs registrering" {
        val hendelseSerde = opprettSerde<Hendelse>()
        val periodeSerde = opprettSerde<PeriodeTilstandV1>()
        val dbNavn = "tilstandsDb"

        val inn = "eventlogTopic"
        val ut = "periodeTopic"
        val topology = topology(
            opprettStreamsBuilder(dbNavn, periodeSerde),
            dbNavn,
            inn,
            ut
        )

        val testDriver = TopologyTestDriver(topology, kafkaStreamProperties)
        val eventlogTopic = testDriver.createInputTopic(
            inn,
            Serdes.String().serializer(),
            hendelseSerde.serializer()
        )
        val utTopic = testDriver.createOutputTopic(
            ut,
            Serdes.String().deserializer(),
            periodeSerde.deserializer()
        )
        val start = Hendelse(
            "12345678901",
            Metadata(
                UUID.randomUUID(),
                Instant.now(),
                Bruker(BrukerType.SYSTEM, "test"),
                "unit-test",
                "tester"),
            Start()
        )
        eventlogTopic.pipeInput(start.identitetsnummer, start)
        val periode = utTopic.readKeyValue()
        periode.key shouldBe start.identitetsnummer
        periode.value.identitetsnummer shouldBe start.identitetsnummer
        periode.value.fraOgMed shouldBe start.metadata.tidspunkt
        periode.value.tilOgMed shouldBe null
        periode.value.id.shouldNotBeNull()
    }
})

