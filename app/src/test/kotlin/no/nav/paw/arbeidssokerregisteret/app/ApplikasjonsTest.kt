package no.nav.paw.arbeidssokerregisteret.app

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Situasjon
import no.nav.paw.arbeidssokerregisteret.app.tilstand.TilstandSerde
import no.nav.paw.arbeidssokerregisteret.intern.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.intern.v1.BrukerType
import no.nav.paw.arbeidssokerregisteret.intern.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.intern.v1.Startet
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.TopologyTestDriver
import java.time.Instant
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode as ApiPeriode


class ApplikasjonsTest : StringSpec({
    "Verifiser at vi oppretter en ny periode ved førstegangs registrering" {
        val hendelseSerde = opprettSerde<SpecificRecord>()
        val periodeSerde = opprettSerde<Periode>()
        val situasjonSerde = opprettSerde<Situasjon>()
        val tilstandSerde = TilstandSerde()
        val dbNavn = "tilstandsDb"

        val eventlogTopicNavn = "eventlogTopic"
        val periodeTopicNavn = "periodeTopic"
        val situasjonTopicNavn = "situasjonTopic"
        val topology = topology(
            builder = opprettStreamsBuilder(dbNavn, tilstandSerde),
            dbNavn = dbNavn,
            innTopic = eventlogTopicNavn,
            periodeTopic = periodeTopicNavn,
            situasjonTopic = situasjonTopicNavn
        )

        val testDriver = TopologyTestDriver(topology, kafkaStreamProperties)
        val eventlogTopic = testDriver.createInputTopic(
            eventlogTopicNavn,
            Serdes.Long().serializer(),
            hendelseSerde.serializer()
        )
        val periodeTopic = testDriver.createOutputTopic(
            periodeTopicNavn,
            Serdes.Long().deserializer(),
            periodeSerde.deserializer()
        )
        val situasjonTopic = testDriver.createOutputTopic(
            situasjonTopicNavn,
            Serdes.Long().deserializer(),
            situasjonSerde.deserializer()
        )
        val start = Startet(
            "12345678901",
            Metadata(
                Instant.now(),
                Bruker(BrukerType.SYSTEM, "test"),
                "unit-test",
                "tester")
        )
        eventlogTopic.pipeInput(start.identitetsnummer.hashCode().toLong(), start)
        val periode = periodeTopic.readKeyValue()
        periode.key shouldBe start.identitetsnummer.hashCode().toLong()
        periode.value.identitetsnummer shouldBe start.identitetsnummer
        periode.value.startet.tidspunkt shouldBe start.metadata.tidspunkt
        periode.value.startet.aarsak shouldBe start.metadata.aarsak
        periode.value.startet.utfoertAv.type.name shouldBe start.metadata.utfoertAv.type.name
        periode.value.startet.utfoertAv.id shouldBe start.metadata.utfoertAv.id
        periode.value.avsluttet shouldBe null
        periode.value.id.shouldNotBeNull()
    }
})

