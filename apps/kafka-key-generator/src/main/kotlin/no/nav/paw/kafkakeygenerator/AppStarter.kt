package no.nav.paw.kafkakeygenerator

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.client.config.AZURE_M2M_CONFIG
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.kafkakeygenerator.config.AUTHENTICATION_CONFIG
import no.nav.paw.kafkakeygenerator.config.AuthenticationConfig
import no.nav.paw.kafkakeygenerator.config.DATABASE_CONFIG
import no.nav.paw.kafkakeygenerator.config.PDL_CLIENT_CONFIG
import no.nav.paw.kafkakeygenerator.database.createDataSource
import no.nav.paw.kafkakeygenerator.database.flywayMigrate
import no.nav.paw.kafkakeygenerator.handler.KafkaConsumerErrorHandler
import no.nav.paw.kafkakeygenerator.handler.KafkaConsumerRecordHandler
import no.nav.paw.kafkakeygenerator.ktor.initKtorServer
import no.nav.paw.kafkakeygenerator.merge.MergeDetector
import no.nav.paw.kafkakeygenerator.pdl.PdlIdentitesTjeneste
import no.nav.paw.kafkakeygenerator.pdl.opprettPdlKlient
import no.nav.paw.kafkakeygenerator.repository.KafkaKeysAuditRepository
import no.nav.paw.kafkakeygenerator.repository.KafkaKeysRepository
import no.nav.paw.pdl.PdlClient
import org.jetbrains.exposed.sql.Database
import javax.sql.DataSource

fun main() {
    val dataSource = createDataSource(loadNaisOrLocalConfiguration(DATABASE_CONFIG))
    val pdlKlient = opprettPdlKlient(
        loadNaisOrLocalConfiguration(PDL_CLIENT_CONFIG),
        loadNaisOrLocalConfiguration(AZURE_M2M_CONFIG)
    )
    startApplikasjon(
        loadNaisOrLocalConfiguration(AUTHENTICATION_CONFIG),
        dataSource,
        pdlKlient
    )
}

fun startApplikasjon(
    authenticationConfig: AuthenticationConfig,
    dataSource: DataSource,
    pdlKlient: PdlClient
) {
    val database = Database.connect(dataSource)
    val healthIndicatorRepository = HealthIndicatorRepository()
    val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    flywayMigrate(dataSource)
    val kafkaKeys = KafkaKeys(database)
    val kafkaConsumerRecordHandler = KafkaConsumerRecordHandler(
        kafkaKeysRepository = KafkaKeysRepository(database),
        kafkaKeysAuditRepository = KafkaKeysAuditRepository(database)
    )
    val kafkaConsumerErrorHandler = KafkaConsumerErrorHandler(healthIndicatorRepository)
    val pdlIdTjeneste = PdlIdentitesTjeneste(pdlKlient)
    val applikasjon = Applikasjon(
        kafkaKeys,
        pdlIdTjeneste
    )
    val mergeDetector = MergeDetector(
        pdlIdTjeneste,
        kafkaKeys
    )
    initKtorServer(
        authenticationConfig = authenticationConfig,
        prometheusMeterRegistry = prometheusMeterRegistry,
        healthIndicatorRepository = healthIndicatorRepository,
        kafkaConsumerRecordHandler = kafkaConsumerRecordHandler,
        kafkaConsumerErrorHandler = kafkaConsumerErrorHandler,
        applikasjon = applikasjon,
        mergeDetector = mergeDetector
    ).start(wait = true)
}
