package no.nav.paw.bqadapter

import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseDeserializer
import no.nav.paw.bqadapter.bigquery.createBigQueryContext
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.health.route.healthRoutes
import no.nav.paw.kafka.config.KAFKA_CONFIG_WITH_SCHEME_REG
import no.nav.paw.kafka.config.KafkaConfig
import no.nav.paw.kafka.consumer.CommittingKafkaConsumerWrapper
import no.nav.paw.kafka.factory.KafkaFactory
import no.nav.paw.kafka.plugin.KafkaConsumerPlugin
import no.nav.paw.metrics.route.metricsRoutes
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.LongDeserializer
import org.slf4j.LoggerFactory
import java.nio.file.Paths

val appLogger = LoggerFactory.getLogger("app")

val periodeIdSaltPath = Paths.get("/var/run/secrets/periode_id/enc_periode")
val hendelseIdentSaltPath = Paths.get("/var/run/secrets/ident/enc_hendelse")

fun main() {
    appLogger.info("Starter app...")
    val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val healthIndicatorRepository = HealthIndicatorRepository()
    val encoder = Encoder(
        identSalt = hendelseIdentSaltPath.toFile().readBytes(),
        periodeIdSalt = periodeIdSaltPath.toFile().readBytes()
    )
    val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_CONFIG_WITH_SCHEME_REG)
    val kafkaFactory = KafkaFactory(kafkaConfig)
    val consumer = kafkaFactory.createConsumer<Long, ByteArray>(
        groupId = "bq-consumer-v6",
        clientId = "bq-consumer-v3-${System.currentTimeMillis()}",
        keyDeserializer = LongDeserializer::class,
        valueDeserializer = ByteArrayDeserializer::class,
        autoCommit = false,
        autoOffsetReset = "earliest",
        maxPollrecords = 1000
    )

    appLogger.info("Lastet encoder: $encoder")
    val appConfig = appConfig
    appLogger.info("App config: $appConfig")
    val bigqueryContext = createBigQueryContext(
        project = appConfig.bigqueryProject,
        encoder = encoder,
        hendelserDeserializer = HendelseDeserializer(),
        periodeDeserializer = kafkaFactory.kafkaAvroDeSerializer<Periode>(),
    )
    embeddedServer(factory = Netty, port = 8080) {
        install(KafkaConsumerPlugin<Long, ByteArray>("application_consumer")) {
            onConsume = bigqueryContext::handleRecords
            kafkaConsumerWrapper = CommittingKafkaConsumerWrapper(
                topics = listOf(HENDELSE_TOPIC, PERIODE_TOPIC),
                consumer = consumer,
                exceptionHandler = { throwable ->
                    appLogger.error("Error in consumer", throwable)
                    throw throwable
                }
            )
        }
        routing {
            metricsRoutes(prometheusMeterRegistry)
            healthRoutes(healthIndicatorRepository)
        }
    }.start(wait = true)
}