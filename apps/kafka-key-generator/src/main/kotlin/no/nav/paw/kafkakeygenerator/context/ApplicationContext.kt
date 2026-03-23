package no.nav.paw.kafkakeygenerator.context

import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.micrometer.core.instrument.binder.MeterBinder
import io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseSerializer
import no.nav.paw.client.config.AZURE_M2M_CONFIG
import no.nav.paw.client.config.AzureAdM2MConfig
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.database.config.DATABASE_CONFIG
import no.nav.paw.database.config.DatabaseConfig
import no.nav.paw.database.factory.createHikariDataSource
import no.nav.paw.health.model.HealthStatus
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.identitet.internehendelser.IdentitetHendelse
import no.nav.paw.identitet.internehendelser.IdentitetHendelseSerializer
import no.nav.paw.kafka.config.KAFKA_CONFIG_WITH_SCHEME_REG
import no.nav.paw.kafka.config.KafkaConfig
import no.nav.paw.kafka.factory.KafkaFactory
import no.nav.paw.kafka.handler.ConsumerExceptionHandler
import no.nav.paw.kafka.listener.HwmConsumerRebalanceListener
import no.nav.paw.kafka.service.KafkaHwmService
import no.nav.paw.kafkakeygenerator.client.PawHendelseloggKafkaProducer
import no.nav.paw.kafkakeygenerator.client.PawIdentitetKafkaProducer
import no.nav.paw.kafkakeygenerator.client.PawPeriodeKafkaConsumer
import no.nav.paw.kafkakeygenerator.client.PdlAktorKafkaConsumer
import no.nav.paw.kafkakeygenerator.client.PdlRestConsumer
import no.nav.paw.kafkakeygenerator.config.APPLICATION_CONFIG
import no.nav.paw.kafkakeygenerator.config.ApplicationConfig
import no.nav.paw.kafkakeygenerator.config.PDL_CLIENT_CONFIG
import no.nav.paw.kafkakeygenerator.config.PdlClientConfig
import no.nav.paw.kafkakeygenerator.config.SERVER_CONFIG
import no.nav.paw.kafkakeygenerator.config.ServerConfig
import no.nav.paw.kafkakeygenerator.handler.HealthIndicatorConsumerExceptionHandler
import no.nav.paw.kafkakeygenerator.service.IdentitetResponseService
import no.nav.paw.kafkakeygenerator.service.IdentitetService
import no.nav.paw.kafkakeygenerator.service.KafkaKeysService
import no.nav.paw.kafkakeygenerator.service.KonfliktService
import no.nav.paw.kafkakeygenerator.utils.createPdlClient
import no.nav.paw.security.authentication.config.SECURITY_CONFIG
import no.nav.paw.security.authentication.config.SecurityConfig
import no.nav.person.pdl.aktor.v2.Aktor
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.LongDeserializer
import org.apache.kafka.common.serialization.LongSerializer
import javax.sql.DataSource

data class ApplicationContext(
    val serverConfig: ServerConfig,
    val securityConfig: SecurityConfig,
    val applicationConfig: ApplicationConfig,
    val dataSource: DataSource,
    val prometheusMeterRegistry: PrometheusMeterRegistry,
    val healthIndicatorRepository: HealthIndicatorRepository,
    val identitetService: IdentitetService,
    val identitetResponseService: IdentitetResponseService,
    val konfliktService: KonfliktService,
    val pawPeriodeConsumer: KafkaConsumer<Long, Periode>,
    val pawPeriodeConsumerExceptionHandler: ConsumerExceptionHandler,
    val pawPeriodeConsumerRebalanceListener: HwmConsumerRebalanceListener,
    val pawPeriodeKafkaConsumer: PawPeriodeKafkaConsumer,
    val pdlAktorConsumer: KafkaConsumer<Any, Aktor>,
    val pdlAktorConsumerExceptionHandler: ConsumerExceptionHandler,
    val pdlAktorConsumerRebalanceListener: HwmConsumerRebalanceListener,
    val pdlAktorKafkaConsumer: PdlAktorKafkaConsumer,
    val kafkaKeysService: KafkaKeysService,
    val additionalMeterBinders: List<MeterBinder>
) {
    companion object {
        fun create(): ApplicationContext {
            val serverConfig = loadNaisOrLocalConfiguration<ServerConfig>(SERVER_CONFIG)
            val databaseConfig = loadNaisOrLocalConfiguration<DatabaseConfig>(DATABASE_CONFIG)
            val securityConfig = loadNaisOrLocalConfiguration<SecurityConfig>(SECURITY_CONFIG)
            val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_CONFIG_WITH_SCHEME_REG)
            val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>(APPLICATION_CONFIG)
            val azureAdM2MConfig = loadNaisOrLocalConfiguration<AzureAdM2MConfig>(AZURE_M2M_CONFIG)
            val pdlClientConfig = loadNaisOrLocalConfiguration<PdlClientConfig>(PDL_CLIENT_CONFIG)
            val dataSource = createHikariDataSource(databaseConfig)
            val pdlClient = createPdlClient(pdlClientConfig, azureAdM2MConfig)
            val healthIndicatorRepository = HealthIndicatorRepository()
            val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
            val kafkaFactory = KafkaFactory(kafkaConfig)

            val pawIdentitetKafkaProducer = PawIdentitetKafkaProducer(
                applicationConfig = applicationConfig,
                producer = kafkaFactory.createProducer<Long, IdentitetHendelse>(
                    clientId = applicationConfig.pawIdentitetProducer.clientId,
                    keySerializer = LongSerializer::class,
                    valueSerializer = IdentitetHendelseSerializer::class,
                )
            )
            val pawHendelseloggKafkaProducer = PawHendelseloggKafkaProducer(
                runtimeEnvironment = serverConfig.runtimeEnvironment,
                applicationConfig = applicationConfig,
                producer = kafkaFactory.createProducer<Long, Hendelse>(
                    clientId = applicationConfig.pawHendelseloggProducer.clientId,
                    keySerializer = LongSerializer::class,
                    valueSerializer = HendelseSerializer::class,
                )
            )

            val konfliktService = KonfliktService(
                applicationConfig = applicationConfig,
                pawIdentitetKafkaProducer = pawIdentitetKafkaProducer,
                pawHendelseloggKafkaProducer = pawHendelseloggKafkaProducer
            )
            val identitetService = IdentitetService(
                konfliktService = konfliktService,
                pawIdentitetKafkaProducer = pawIdentitetKafkaProducer,
            )
            val pdlRestConsumer = PdlRestConsumer(pdlClient = pdlClient)
            val kafkaKeysService = KafkaKeysService(
                meterRegistry = prometheusMeterRegistry,
                pdlRestConsumer = pdlRestConsumer,
                identitetService = identitetService
            )
            val identitetResponseService = IdentitetResponseService(
                pdlRestConsumer = pdlRestConsumer
            )
            val pawPeriodeKafkaHwmOperations = KafkaHwmService(
                kafkaConsumerConfig = applicationConfig.pawPeriodeConsumer,
                meterRegistry = prometheusMeterRegistry,
            )
            val pawPeriodeConsumer = kafkaFactory.createKafkaAvroValueConsumer<Long, Periode>(
                groupId = applicationConfig.pawPeriodeConsumer.groupId,
                clientId = applicationConfig.pawPeriodeConsumer.clientId,
                keyDeserializer = LongDeserializer::class
            )
            val pawPeriodeKafkaConsumer = PawPeriodeKafkaConsumer(
                kafkaConsumerConfig = applicationConfig.pawPeriodeConsumer,
                kafkaHwmOperations = pawPeriodeKafkaHwmOperations,
            )
            val pawPeriodeConsumerExceptionHandler = HealthIndicatorConsumerExceptionHandler(
                livenessIndicator = healthIndicatorRepository.livenessIndicator(HealthStatus.HEALTHY),
                readinessIndicator = healthIndicatorRepository.readinessIndicator(HealthStatus.HEALTHY)
            )
            val pawPeriodeConsumerRebalanceListener = HwmConsumerRebalanceListener(
                kafkaConsumerConfig = applicationConfig.pawPeriodeConsumer,
                hwmOperations = pawPeriodeKafkaHwmOperations,
                kafkaConsumer = pawPeriodeConsumer
            )
            val pdlAktorKafkaHwmOperations = KafkaHwmService(
                kafkaConsumerConfig = applicationConfig.pdlAktorConsumer,
                meterRegistry = prometheusMeterRegistry,
            )
            val pdlAktorConsumer = kafkaFactory.createKafkaAvroValueConsumer<Any, Aktor>(
                groupId = applicationConfig.pdlAktorConsumer.groupId,
                clientId = applicationConfig.pdlAktorConsumer.clientId,
                keyDeserializer = KafkaAvroDeserializer::class
            )
            val pdlAktorKafkaConsumer = PdlAktorKafkaConsumer(
                kafkaConsumerConfig = applicationConfig.pdlAktorConsumer,
                kafkaHwmOperations = pdlAktorKafkaHwmOperations,
                identitetService = identitetService
            )
            val pdlAktorConsumerExceptionHandler = HealthIndicatorConsumerExceptionHandler(
                livenessIndicator = healthIndicatorRepository.livenessIndicator(HealthStatus.HEALTHY),
                readinessIndicator = healthIndicatorRepository.readinessIndicator(HealthStatus.HEALTHY)
            )
            val pdlAktorConsumerRebalanceListener = HwmConsumerRebalanceListener(
                kafkaConsumerConfig = applicationConfig.pdlAktorConsumer,
                hwmOperations = pdlAktorKafkaHwmOperations,
                kafkaConsumer = pdlAktorConsumer
            )
            return ApplicationContext(
                serverConfig = serverConfig,
                securityConfig = securityConfig,
                applicationConfig = applicationConfig,
                dataSource = dataSource,
                prometheusMeterRegistry = prometheusMeterRegistry,
                healthIndicatorRepository = healthIndicatorRepository,
                identitetService = identitetService,
                identitetResponseService = identitetResponseService,
                konfliktService = konfliktService,
                pawPeriodeConsumer = pawPeriodeConsumer,
                pawPeriodeConsumerExceptionHandler = pawPeriodeConsumerExceptionHandler,
                pawPeriodeConsumerRebalanceListener = pawPeriodeConsumerRebalanceListener,
                pawPeriodeKafkaConsumer = pawPeriodeKafkaConsumer,
                pdlAktorConsumer = pdlAktorConsumer,
                pdlAktorConsumerExceptionHandler = pdlAktorConsumerExceptionHandler,
                pdlAktorConsumerRebalanceListener = pdlAktorConsumerRebalanceListener,
                pdlAktorKafkaConsumer = pdlAktorKafkaConsumer,
                kafkaKeysService = kafkaKeysService,
                additionalMeterBinders = listOf(
                    KafkaClientMetrics(pawPeriodeConsumer),
                    KafkaClientMetrics(pdlAktorConsumer)
                )
            )
        }
    }
}
