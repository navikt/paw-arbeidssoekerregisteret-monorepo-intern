package no.nav.paw.bqadapter

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.bigquery.Bigquery
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.cloud.bigquery.BigQueryOptions
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.bqadapter.bigquery.deserializers
import no.nav.paw.bqadapter.bigquery.initBqApp
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.health.model.HealthStatus
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
import java.util.concurrent.atomic.AtomicBoolean


val appLogger = LoggerFactory.getLogger("app")

val periodeIdSaltPath = Paths.get("/var/run/secrets/periode_id/enc_periode")
val hendelseIdentSaltPath = Paths.get("/var/run/secrets/ident/enc_hendelse")

fun main() {
    val startupStartedAt = System.nanoTime()
    appLogger.info("Starter app...")
    val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val healthIndicatorRepository = HealthIndicatorRepository()
    val livenessHealthIndicator = healthIndicatorRepository.livenessIndicator(defaultStatus = HealthStatus.HEALTHY)
    val readinessHealthIndicator = healthIndicatorRepository.readinessIndicator(defaultStatus = HealthStatus.UNHEALTHY)

    appLogger.info("Laster salter for pseudonymisering")
    val encoder = Encoder(
        identSalt = hendelseIdentSaltPath.toFile().readBytes(),
        periodeIdSalt = periodeIdSaltPath.toFile().readBytes()
    )
    appLogger.info("Salter for pseudonymisering er lastet")

    appLogger.info("Laster Kafka-konfigurasjon")
    val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_CONFIG_WITH_SCHEME_REG)
    val kafkaFactory = KafkaFactory(kafkaConfig)
    appLogger.info("Oppretter Kafka-consumer")
    val consumer = kafkaFactory.createConsumer<Long, ByteArray>(
        groupId = "bq-consumer-v10",
        clientId = "bq-consumer-v3-${System.currentTimeMillis()}",
        keyDeserializer = LongDeserializer::class,
        valueDeserializer = ByteArrayDeserializer::class,
        autoCommit = false,
        autoOffsetReset = "earliest",
        maxPollrecords = 1000
    )

    val appConfig = appConfig
    appLogger.info("Initialiserer BigQuery")
    val bigqueryAppContext = initBqApp(
        livenessHealthIndicator = livenessHealthIndicator,
        readinessHealthIndicator = readinessHealthIndicator,
        bigquery = BigQueryOptions.getDefaultInstance().getService(),
        project = appConfig.bigqueryProject,
        encoder = encoder,
        deserializers = kafkaFactory.deserializers(),
        bigqueryModel = Bigquery.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            GsonFactory.getDefaultInstance(),
            HttpCredentialsAdapter(ServiceAccountCredentials.getApplicationDefault())
        ).build()
    )
    appLogger.info(
        "BigQuery er initialisert etter {} ms",
        elapsedMillis(startupStartedAt)
    )
    val consumerWrapper = CommittingKafkaConsumerWrapper(
        topics = listOf(
            bigqueryAppContext.topics.hendelseloggTopic,
            bigqueryAppContext.topics.periodeTopic,
            bigqueryAppContext.topics.opplysningerTopic,
            bigqueryAppContext.topics.profileringTopic,
            bigqueryAppContext.topics.egenvurderingTopic,
            bigqueryAppContext.topics.paavnegneavTopic,
            bigqueryAppContext.topics.bekreftelseTopic,
            bigqueryAppContext.topics.bekreftelseHendelseloggTopic
        ),
        consumer = consumer,
        exceptionHandler = { throwable ->
            livenessHealthIndicator.setUnhealthy()
            readinessHealthIndicator.setUnhealthy()
            appLogger.error("Error in consumer", throwable)
        },
        rebalanceListener = CustomRebalancingListener(
            bqDatabase = bigqueryAppContext.bqDatabase,
            topicNames = bigqueryAppContext.topics,
            consumer = consumer
        )
    )
    val firstPollLogged = AtomicBoolean(false)
    appLogger.info("Starter Ktor-server")
    embeddedServer(factory = Netty, port = 8080) {
        install(KafkaConsumerPlugin<Long, ByteArray>("application_consumer")) {
            onConsume = { records ->
                if (firstPollLogged.compareAndSet(false, true)) {
                    appLogger.info("Første Kafka-poll fullført med {} records", records.count())
                }
                bigqueryAppContext.handleRecords(records)
            }
            kafkaConsumerWrapper = consumerWrapper
        }
        routing {
            metricsRoutes(prometheusMeterRegistry)
            healthRoutes(healthIndicatorRepository)
        }
    }.start(wait = true)
}

private fun elapsedMillis(startedAt: Long): Long =
    (System.nanoTime() - startedAt) / 1_000_000