package no.nav.paw.arbeidssokerregisteret.app

import no.nav.paw.arbeidssokerregisteret.app.config.KafkaSourceConfig
import no.nav.paw.arbeidssokerregisteret.app.config.SchemaRegistryConfig
import no.nav.paw.arbeidssokerregisteret.app.funksjoner.ignorerDuplikateStartStoppEventer
import no.nav.paw.arbeidssokerregisteret.intern.StartV1
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.utils.Time
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Named
import org.apache.kafka.streams.kstream.Produced
import org.apache.kafka.streams.state.internals.KeyValueStoreBuilder
import org.apache.kafka.streams.state.internals.RocksDbKeyValueBytesStoreSupplier
import org.slf4j.LoggerFactory

fun main() {
    val kildeConfig = KafkaSourceConfig(System.getenv())

    val streamLogger = LoggerFactory.getLogger("App")
    streamLogger.info("Starter stream")
    val streamsConfig = StreamsConfig(kildeConfig.properties)
    val schemaRegistryConfig = SchemaRegistryConfig(System.getenv())
    val eventSerde: Serde<SpecificRecord> = lagSpecificAvroSerde(schemaRegistryConfig)
    val stateSerde: Serde<PeriodeTilstandV1> = lagSpecificAvroSerde(schemaRegistryConfig)
    val producedWith: Produced<String, SpecificRecord> = Produced.with(Serdes.String(), eventSerde)
    val dbNavn = "tilstandsDb"
    val builder = StreamsBuilder()
    builder
        .addStateStore(
            KeyValueStoreBuilder(
                RocksDbKeyValueBytesStoreSupplier(dbNavn, false),
                Serdes.String(),
                stateSerde,
                Time.SYSTEM
            )
        )
        .stream(
            kildeConfig.eventlogTopic, Consumed.with(
                Serdes.String(),
                eventSerde
            )
        )
        .process(
            ignorerDuplikateStartStoppEventer,
            Named.`as`("filtrer-duplikate-start-stopp-eventer"),
            dbNavn
        )
        .peek { key, value -> streamLogger.info("key: $key, value: $value") }
        .to("output", producedWith)
    val topology = builder.build()
    val kafkaStreams = KafkaStreams(topology, streamsConfig)
    kafkaStreams.setUncaughtExceptionHandler { throwable ->
        streamLogger.error("Uventet feil", throwable)
        StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.SHUTDOWN_APPLICATION
    }
    kafkaStreams.start()
    Thread.sleep(Long.MAX_VALUE)
}


fun StartV1.periodeTilstand() = PeriodeTilstandV1(id, personNummer, timestamp)
