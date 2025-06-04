package no.nav.paw.kafkakeygenerator.context

import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.micrometer.core.instrument.binder.MeterBinder
import io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseDeserializer
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
import no.nav.paw.kafkakeygenerator.config.APPLICATION_CONFIG
import no.nav.paw.kafkakeygenerator.config.ApplicationConfig
import no.nav.paw.kafkakeygenerator.config.PDL_CLIENT_CONFIG
import no.nav.paw.kafkakeygenerator.config.PdlClientConfig
import no.nav.paw.kafkakeygenerator.config.SERVER_CONFIG
import no.nav.paw.kafkakeygenerator.config.ServerConfig
import no.nav.paw.kafkakeygenerator.handler.HealthIndicatorConsumerExceptionHandler
import no.nav.paw.kafkakeygenerator.listener.HwmConsumerRebalanceListener
import no.nav.paw.kafkakeygenerator.merge.MergeDetector
import no.nav.paw.kafkakeygenerator.repository.HwmRepository
import no.nav.paw.kafkakeygenerator.repository.IdentitetHendelseRepository
import no.nav.paw.kafkakeygenerator.repository.IdentitetKonfliktRepository
import no.nav.paw.kafkakeygenerator.repository.IdentitetRepository
import no.nav.paw.kafkakeygenerator.repository.KafkaKeysAuditRepository
import no.nav.paw.kafkakeygenerator.repository.KafkaKeysIdentitetRepository
import no.nav.paw.kafkakeygenerator.repository.KafkaKeysRepository
import no.nav.paw.kafkakeygenerator.repository.PeriodeRepository
import no.nav.paw.kafkakeygenerator.service.IdentitetHendelseService
import no.nav.paw.kafkakeygenerator.service.IdentitetKonfliktService
import no.nav.paw.kafkakeygenerator.service.IdentitetService
import no.nav.paw.kafkakeygenerator.service.KafkaHwmService
import no.nav.paw.kafkakeygenerator.service.KafkaKeysService
import no.nav.paw.kafkakeygenerator.service.PawHendelseKafkaConsumerService
import no.nav.paw.kafkakeygenerator.service.PawPeriodeKafkaConsumerService
import no.nav.paw.kafkakeygenerator.service.PdlAktorKafkaConsumerService
import no.nav.paw.kafkakeygenerator.service.PdlService
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
    val identitetKonfliktService: IdentitetKonfliktService,
    val identitetHendelseService: IdentitetHendelseService,
    val pawHendelseKafkaConsumer: KafkaConsumer<Long, Hendelse>,
    val pawHendelseConsumerExceptionHandler: ConsumerExceptionHandler,
    val pawHendelseKafkaConsumerService: PawHendelseKafkaConsumerService,
    val pawPeriodeConsumer: KafkaConsumer<Long, Periode>,
    val pawPeriodeConsumerExceptionHandler: ConsumerExceptionHandler,
    val pawPeriodeConsumerRebalanceListener: HwmConsumerRebalanceListener,
    val pawPeriodeKafkaConsumerService: PawPeriodeKafkaConsumerService,
    val pdlAktorConsumer: KafkaConsumer<Any, Aktor>,
    val pdlAktorConsumerExceptionHandler: ConsumerExceptionHandler,
    val pdlAktorConsumerRebalanceListener: HwmConsumerRebalanceListener,
    val pdlAktorKafkaConsumerService: PdlAktorKafkaConsumerService,
    val kafkaKeysService: KafkaKeysService,
    val mergeDetector: MergeDetector,
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

            val hwmRepository = HwmRepository()
            val kafkaKeysIdentitetRepository = KafkaKeysIdentitetRepository()
            val kafkaKeysRepository = KafkaKeysRepository()
            val kafkaKeysAuditRepository = KafkaKeysAuditRepository()
            val identitetRepository = IdentitetRepository()
            val identitetKonfliktRepository = IdentitetKonfliktRepository()
            val identitetHendelseRepository = IdentitetHendelseRepository()
            val periodeRepository = PeriodeRepository()

            val pawIdentitetProducer = kafkaFactory.createProducer<Long, IdentitetHendelse>(
                clientId = applicationConfig.pawIdentitetProducer.clientId,
                keySerializer = LongSerializer::class,
                valueSerializer = IdentitetHendelseSerializer::class,
            )

            val identitetHendelseService = IdentitetHendelseService(
                applicationConfig = applicationConfig,
                identitetHendelseRepository = identitetHendelseRepository,
                pawIdentitetProducer = pawIdentitetProducer
            )
            val identitetKonfliktService = IdentitetKonfliktService(
                identitetRepository = identitetRepository,
                identitetKonfliktRepository = identitetKonfliktRepository,
                periodeRepository = periodeRepository,
                identitetHendelseService = identitetHendelseService
            )
            val identitetService = IdentitetService(
                identitetRepository = identitetRepository,
                identitetKonfliktService = identitetKonfliktService,
                identitetHendelseService = identitetHendelseService,
                kafkaKeysIdentitetRepository = kafkaKeysIdentitetRepository
            )
            val pawHendelseKafkaConsumerService = PawHendelseKafkaConsumerService(
                meterRegistry = prometheusMeterRegistry,
                kafkaKeysIdentitetRepository = kafkaKeysIdentitetRepository,
                kafkaKeysRepository = kafkaKeysRepository,
                kafkaKeysAuditRepository = kafkaKeysAuditRepository
            )
            val pdlService = PdlService(pdlClient = pdlClient)
            val kafkaKeysService = KafkaKeysService(
                meterRegistry = prometheusMeterRegistry,
                kafkaKeysRepository = kafkaKeysRepository,
                pdlService = pdlService,
                identitetService = identitetService
            )
            val mergeDetector = MergeDetector(
                kafkaKeysRepository = kafkaKeysRepository,
                pdlService = pdlService
            )
            val pawHendelseKafkaConsumer = kafkaFactory.createConsumer(
                groupId = applicationConfig.pawHendelseConsumer.groupId,
                clientId = applicationConfig.pawHendelseConsumer.clientId,
                keyDeserializer = LongDeserializer::class,
                valueDeserializer = HendelseDeserializer::class
            )
            val pawHendelseConsumerExceptionHandler = HealthIndicatorConsumerExceptionHandler(
                livenessIndicator = healthIndicatorRepository.livenessIndicator(HealthStatus.HEALTHY),
                readinessIndicator = healthIndicatorRepository.readinessIndicator(HealthStatus.HEALTHY)
            )
            val pawPeriodeKafkaHwmOperations = KafkaHwmService(
                kafkaConsumerConfig = applicationConfig.pawPeriodeConsumer,
                hwmRepository = hwmRepository
            )
            val pawPeriodeConsumer = kafkaFactory.createKafkaAvroValueConsumer<Long, Periode>(
                groupId = applicationConfig.pawPeriodeConsumer.groupId,
                clientId = applicationConfig.pawPeriodeConsumer.clientId,
                keyDeserializer = LongDeserializer::class
            )
            val pawPeriodeKafkaConsumerService = PawPeriodeKafkaConsumerService(
                kafkaConsumerConfig = applicationConfig.pawPeriodeConsumer,
                hwmOperations = pawPeriodeKafkaHwmOperations,
                periodeRepository = periodeRepository
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
                hwmRepository = hwmRepository
            )
            val pdlAktorConsumer = kafkaFactory.createKafkaAvroValueConsumer<Any, Aktor>(
                groupId = applicationConfig.pdlAktorConsumer.groupId,
                clientId = applicationConfig.pdlAktorConsumer.clientId,
                keyDeserializer = KafkaAvroDeserializer::class
            )
            val pdlAktorKafkaConsumerService = PdlAktorKafkaConsumerService(
                kafkaConsumerConfig = applicationConfig.pdlAktorConsumer,
                hwmOperations = pdlAktorKafkaHwmOperations,
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
                identitetKonfliktService = identitetKonfliktService,
                identitetHendelseService = identitetHendelseService,
                pawHendelseKafkaConsumer = pawHendelseKafkaConsumer,
                pawHendelseConsumerExceptionHandler = pawHendelseConsumerExceptionHandler,
                pawHendelseKafkaConsumerService = pawHendelseKafkaConsumerService,
                pawPeriodeConsumer = pawPeriodeConsumer,
                pawPeriodeConsumerExceptionHandler = pawPeriodeConsumerExceptionHandler,
                pawPeriodeConsumerRebalanceListener = pawPeriodeConsumerRebalanceListener,
                pawPeriodeKafkaConsumerService = pawPeriodeKafkaConsumerService,
                pdlAktorConsumer = pdlAktorConsumer,
                pdlAktorConsumerExceptionHandler = pdlAktorConsumerExceptionHandler,
                pdlAktorConsumerRebalanceListener = pdlAktorConsumerRebalanceListener,
                pdlAktorKafkaConsumerService = pdlAktorKafkaConsumerService,
                kafkaKeysService = kafkaKeysService,
                mergeDetector = mergeDetector,
                additionalMeterBinders = listOf(KafkaClientMetrics(pawHendelseKafkaConsumer))
            )
        }
    }
}
