package no.nav.paw.arbeidssoekerregisteret.app

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import io.micrometer.core.instrument.binder.kafka.KafkaStreamsMetrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import kotlinx.coroutines.runBlocking
import no.nav.paw.arbeidssokerregisteret.app.helse.Helse
import no.nav.paw.arbeidssokerregisteret.app.helse.initKtor
import no.nav.paw.config.env.NaisEnv
import no.nav.paw.config.env.currentNaisEnv
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.config.kafka.KafkaConfig
import no.nav.paw.config.kafka.streams.KafkaStreamsFactory
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.state.Stores
import org.slf4j.LoggerFactory
import no.nav.paw.config.kafka.KAFKA_STREAMS_CONFIG_WITH_SCHEME_REG
import no.nav.paw.kafkakeygenerator.client.KafkaKeysResponse
import no.nav.paw.kafkakeygenerator.client.createKafkaKeyGeneratorClient
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler

const val partitionCount: Int = 6

const val periodeTopic = "paw.arbeidssokerperioder-v1"
val applicationConfiguration: ApplicationConfiguration get() =
    loadNaisOrLocalConfiguration<ApplicationConfiguration>("application_configuration.toml")

typealias kafkaKeyFunction = (String) -> KafkaKeysResponse?

fun formidlingsGruppeTopic(env: NaisEnv) =
    "teamarenanais.aapen-arena-formidlingsgruppeendret-v1-${if (env == NaisEnv.ProdGCP) "p" else "q"}"

fun main() {
    val logger = LoggerFactory.getLogger("app")
    logger.info("Starter: {}", ApplicationInfo.id)
    val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_STREAMS_CONFIG_WITH_SCHEME_REG)
    val appCfg = applicationConfiguration
    val idAndRecordKeyFunction = with(createKafkaKeyGeneratorClient()) {
        { identitetsnummer: String ->
            runBlocking { getIdAndKeyOrNull(identitetsnummer) }
        }
    }
    val streamsConfig = KafkaStreamsFactory(
        applicationIdSuffix = appCfg.applicationStreamVersion,
        config = kafkaConfig
    )
        .withDefaultKeySerde(Serdes.LongSerde::class)
        .withDefaultValueSerde(SpecificAvroSerde::class)
    val streamsBuilder = StreamsBuilder()
        .addStateStore(
            Stores.keyValueStoreBuilder(
                Stores.persistentKeyValueStore("aktivePerioder"),
                Serdes.Long(),
                streamsConfig.createSpecificAvroSerde()
            )
        )
    val topology = streamsBuilder.appTopology(
        prometheusMeterRegistry,
        "aktivePerioder",
        idAndRecordKeyFunction,
        periodeTopic,
        formidlingsGruppeTopic(currentNaisEnv),
        appCfg.hendelseloggTopic
    )
    val kafkaStreams = KafkaStreams(
        topology,
        StreamsConfig(streamsConfig.properties)
    )
    kafkaStreams.setUncaughtExceptionHandler { throwable ->
        logger.error("Uventet feil: ${throwable.message}", throwable)
        StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.SHUTDOWN_APPLICATION
    }
    kafkaStreams.start()
    val helse = Helse(kafkaStreams)
    val streamMetrics = KafkaStreamsMetrics(kafkaStreams)
    initKtor(
        kafkaStreamsMetrics = streamMetrics,
        prometheusRegistry = prometheusMeterRegistry,
        helse = helse
    ).start(wait = true)
    logger.info("Avsluttet")
}

