package no.nav.paw.arbeidssokerregisteret.app

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.Start
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.serialization.Serializer
import org.apache.kafka.common.utils.Time
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.state.internals.InMemoryKeyValueBytesStoreSupplier
import org.apache.kafka.streams.state.internals.KeyValueStoreBuilder
import java.nio.ByteBuffer
import java.time.Instant
import java.util.*

val SCHEMA_REGISTRY_SCOPE = ApplikasjonsTest::class.java.getName();


class ApplikasjonsTest : StringSpec({
    "test av applikasjon" {
        val hendelseSerde: Serde<Hendelse> = GenericSerde()
        val periodeSerde: Serde<PeriodeTilstandV1> = GenericSerde()

        val builder = StreamsBuilder()
        builder.addStateStore(
            KeyValueStoreBuilder(
                InMemoryKeyValueBytesStoreSupplier("tilstandsDb"),
                Serdes.String(),
                periodeSerde,
                Time.SYSTEM
            )
        )
        val topology = topology(
            builder,
            "tilstandsDb",
            "eventlogTopic",
            "periodeTopic"
        )
        val props = Properties()
        props[StreamsConfig.APPLICATION_ID_CONFIG] = "test"
        props[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = "dummy:1234"
        props[StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG] = Serdes.String().javaClass.name
        props[StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG] = GenericSerde::class.java.name
        props["auto.register.schemas"] = "true"
        props["schema.registry.url"] = "mock://$SCHEMA_REGISTRY_SCOPE"

        val testDriver = TopologyTestDriver(topology, props)
        val eventlog = testDriver.createInputTopic(
            "eventlogTopic",
            Serdes.String().serializer(),
            hendelseSerde.serializer()
        )
        val ut = testDriver.createOutputTopic(
            "periodeTopic",
            Serdes.String().deserializer(),
            periodeSerde.deserializer()
        )
        val start = Hendelse(
            UUID.randomUUID(),
            "12345678901",
            Instant.now(),
            "JUNIT",
            "test",
            Start()
        )
        eventlog.pipeInput(start.foedselsnummer, start)
        val periode = ut.readKeyValue()
        periode.key shouldBe start.foedselsnummer
        periode.value.foedselsnummer shouldBe start.foedselsnummer
        periode.value.fraOgMed shouldBe start.timestamp
        periode.value.tilOgMed shouldBe null
        periode.value.id.shouldNotBeNull()
    }
})