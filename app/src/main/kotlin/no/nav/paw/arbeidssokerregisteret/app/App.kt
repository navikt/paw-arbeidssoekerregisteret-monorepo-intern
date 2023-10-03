package no.nav.paw.arbeidssokerregisteret.app

import io.confluent.kafka.schemaregistry.avro.AvroSchema
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import io.confluent.kafka.serializers.KafkaAvroSerializer
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import io.confluent.kafka.streams.serdes.avro.GenericAvroSerde
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import no.nav.paw.arbeidssokerregisteret.app.config.helpers.konfigVerdi
import no.nav.paw.arbeidssokerregisteret.app.config.nais.KAFKA_BROKERS
import no.nav.paw.arbeidssokerregisteret.intern.StartV1
import no.nav.paw.arbeidssokerregisteret.intern.StoppV1
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.utils.Time
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Named
import org.apache.kafka.streams.kstream.Produced
import org.apache.kafka.streams.processor.api.Processor
import org.apache.kafka.streams.processor.api.ProcessorContext
import org.apache.kafka.streams.processor.api.ProcessorSupplier
import org.apache.kafka.streams.processor.api.Record
import org.apache.kafka.streams.state.KeyValueStore
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
    val eventSerde = SpecificAvroSerde<SpecificRecord>(schemaRegistry).also {
        it.configure(mapOf(
            KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG to true,
            KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS to true,
            KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG to "http://localhost:8082"
            ), false)
    }
    val stateSerde = SpecificAvroSerde<PeriodeTilstandV1>(schemaRegistry).also {
        it.configure(mapOf(
            KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG to true,
            KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS to true,
            KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG to "http://localhost:8082"
        ), false)
    }
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
        .stream(hendelseTopic, Consumed.with(
            Serdes.String(),
            SpecificAvroSerde(schemaRegistry)
        ))
        .process(
            supplier,
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

val supplier: ProcessorSupplier<String, SpecificRecord, String, SpecificRecord> = ProcessorSupplier {
    FiltrerDuplikateStartStoppEventer("tilstandsDb")
}

class FiltrerDuplikateStartStoppEventer(private val tilstandsDbNavn: String) :
    Processor<String, SpecificRecord, String, SpecificRecord> {
    private var tilstandsDb: KeyValueStore<String, PeriodeTilstandV1>? = null
    private var context: ProcessorContext<String, SpecificRecord>? = null
    private val logger = LoggerFactory.getLogger("stream_filter_logger")

    override fun init(context: ProcessorContext<String, SpecificRecord>?) {
        super.init(context)
        this.context = context
        tilstandsDb = context?.getStateStore(tilstandsDbNavn)
    }

    override fun process(record: Record<String, SpecificRecord>?) {
        requireNotNull(tilstandsDb) { "TilstandsDb er ikke initialisert" }
        requireNotNull(context) { "Context er ikke initialisert" }
        when (val value = record?.value()) {
            null -> return
            is StartV1 -> tilstandsDb?.putIfAbsent(value.personNummer, value.periodeTilstand())
                .also {
                    if (it == null) {
                        context?.forward(record)
                        logger.info("${value.personNummer} start event")
                    } else {
                        logger.info("${value.personNummer} Duplikat start event")
                    }
                }

            is StoppV1 -> tilstandsDb?.delete(value.personNummer)
                .also {
                    if (it != null) {
                        context?.forward(record)
                        logger.info("${value.personNummer} Stopp event")
                    } else {
                        logger.info("${value.personNummer} Duplikat stopp event")
                    }
                }

            else -> {
                logger.info("Ukjent event: $value, class=${value::class.qualifiedName}")
                context?.forward(record)
            }
        }
    }
}

fun StartV1.periodeTilstand() = PeriodeTilstandV1(id, personNummer, timestamp)
