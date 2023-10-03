package no.nav.paw.arbeidssokerregisteret.app

import io.confluent.kafka.schemaregistry.avro.AvroSchema
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import io.confluent.kafka.serializers.KafkaAvroSerializer
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import no.nav.paw.arbeidssokerregisteret.app.config.SchemaRegistryConfig
import no.nav.paw.arbeidssokerregisteret.app.config.helpers.konfigVerdi
import no.nav.paw.arbeidssokerregisteret.app.config.nais.KAFKA_BROKERS
import no.nav.paw.arbeidssokerregisteret.app.funksjoner.ignorerDuplikateStartStoppEventer
import no.nav.paw.arbeidssokerregisteret.intern.StartV1
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.utils.Time
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Named
import org.apache.kafka.streams.kstream.Produced
import org.apache.kafka.streams.state.internals.KeyValueStoreBuilder
import org.apache.kafka.streams.state.internals.RocksDbKeyValueBytesStoreSupplier
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.TimeUnit

fun main() {
    val (hendelseTopic, streamsProperties) = with(System.getenv()) {
        "input" to
                Properties().apply {
                    put(StreamsConfig.APPLICATION_ID_CONFIG, "arbeidssokerregisteret-eventlog-v1")
                    put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, konfigVerdi<String>(KAFKA_BROKERS))
                    put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().javaClass.name)
                    put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, SpecificAvroSerde::class.java.name)
                    put("value.converter.schema.registry.url", "http://localhost:8082")
                    put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, "http://localhost:8082")
                    put(KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS, true)
                    put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true)
                }
    }
    val schemaRegistry = CachedSchemaRegistryClient("http://localhost:8082", 100)
    schemaRegistry.register("start-v1", AvroSchema(StartV1.`SCHEMA$`))
    schemaRegistry.register("PeriodeTilstandV1", AvroSchema(PeriodeTilstandV1.`SCHEMA$`))
    val producer = with(System.getenv()) {
        KafkaProducer<String, Any>(
            Properties().apply {
                put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, Serdes.String().serializer().javaClass.name)
                put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer::class.java.name)
                put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, konfigVerdi<String>(KAFKA_BROKERS))
                put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, "http://localhost:8082")
                put(KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS, true)
                put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true)
            }
        )
    }
    producer.send(ProducerRecord(hendelseTopic, "1", StartV1("1", "12345678901", 1)))
        .get(5, TimeUnit.SECONDS)
    val uuid = UUID.randomUUID()
    producer.send(ProducerRecord(hendelseTopic, uuid.toString(), StartV1(uuid.toString(), "12345678903", 2)))
        .get(5, TimeUnit.SECONDS)
    producer.flush()
    producer.close()
    val streamLogger = LoggerFactory.getLogger("App")
    streamLogger.info("Starter stream")
    val streamsConfig = StreamsConfig(streamsProperties)
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
            hendelseTopic, Consumed.with(
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
    KafkaStreams(topology, streamsConfig).start()
    streamLogger.info("Venter...")
    Thread.sleep(Long.MAX_VALUE)
}


fun StartV1.periodeTilstand() = PeriodeTilstandV1(id, personNummer, timestamp)
