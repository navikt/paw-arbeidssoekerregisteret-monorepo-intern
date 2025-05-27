package no.nav.paw.kafkakeygenerator.context

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.logging.LoggingMeterRegistry
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.identitet.internehendelser.IdentitetHendelse
import no.nav.paw.identitet.internehendelser.IdentitetHendelseDeserializer
import no.nav.paw.identitet.internehendelser.IdentitetHendelseSerializer
import no.nav.paw.kafkakeygenerator.config.APPLICATION_CONFIG
import no.nav.paw.kafkakeygenerator.config.ApplicationConfig
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
import no.nav.paw.kafkakeygenerator.service.KafkaHwmOperations
import no.nav.paw.kafkakeygenerator.service.KafkaHwmService
import no.nav.paw.kafkakeygenerator.service.KafkaKeysService
import no.nav.paw.kafkakeygenerator.service.PawHendelseKafkaConsumerService
import no.nav.paw.kafkakeygenerator.service.PdlAktorKafkaConsumerService
import no.nav.paw.kafkakeygenerator.service.PdlService
import no.nav.paw.kafkakeygenerator.test.buildPostgresDataSource
import no.nav.paw.kafkakeygenerator.test.genererResponse
import no.nav.paw.kafkakeygenerator.test.runAsSql
import no.nav.paw.kafkakeygenerator.vo.ArbeidssoekerId
import no.nav.paw.kafkakeygenerator.vo.CallId
import no.nav.paw.kafkakeygenerator.vo.Either
import no.nav.paw.kafkakeygenerator.vo.Failure
import no.nav.paw.kafkakeygenerator.vo.Identitetsnummer
import no.nav.paw.pdl.PdlClient
import org.apache.kafka.clients.producer.Producer
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import java.util.*
import javax.sql.DataSource

class TestContext private constructor(
    val serializer: IdentitetHendelseSerializer = IdentitetHendelseSerializer(),
    val deserializer: IdentitetHendelseDeserializer = IdentitetHendelseDeserializer(),
    val applicationConfig: ApplicationConfig = loadNaisOrLocalConfiguration(APPLICATION_CONFIG),
    val dataSource: DataSource,
    val initSql: List<String> = emptyList(),
    val pdlClient: PdlClient = pdlClient(),
    val meterRegistry: MeterRegistry = LoggingMeterRegistry(),
    val hwmRepository: HwmRepository = HwmRepository(),
    val kafkaKeysRepository: KafkaKeysRepository = KafkaKeysRepository(),
    val kafkaKeysAuditRepository: KafkaKeysAuditRepository = KafkaKeysAuditRepository(),
    val kafkaKeysIdentitetRepository: KafkaKeysIdentitetRepository = KafkaKeysIdentitetRepository(),
    val identitetRepository: IdentitetRepository = IdentitetRepository(),
    val periodeRepository: PeriodeRepository = PeriodeRepository(),
    val identitetKonfliktRepository: IdentitetKonfliktRepository = IdentitetKonfliktRepository(),
    val identitetHendelseRepository: IdentitetHendelseRepository = IdentitetHendelseRepository(),
    val pawIdentitetProducerMock: Producer<Long, IdentitetHendelse> = mockk<Producer<Long, IdentitetHendelse>>(),
    val identitetHendelseService: IdentitetHendelseService = IdentitetHendelseService(
        applicationConfig = applicationConfig,
        identitetHendelseRepository = identitetHendelseRepository,
        pawIdentitetProducer = pawIdentitetProducerMock
    ),
    val identitetKonfliktService: IdentitetKonfliktService = IdentitetKonfliktService(
        identitetRepository = identitetRepository,
        identitetKonfliktRepository = identitetKonfliktRepository,
        periodeRepository = periodeRepository,
        identitetHendelseService = identitetHendelseService
    ),
    val identitetService: IdentitetService = IdentitetService(
        identitetRepository = identitetRepository,
        identitetKonfliktService = identitetKonfliktService,
        identitetHendelseService = identitetHendelseService
    ),
    val pdlService: PdlService = PdlService(pdlClient),
    val kafkaKeysService: KafkaKeysService = KafkaKeysService(
        meterRegistry = meterRegistry,
        kafkaKeysRepository = kafkaKeysRepository,
        pdlService = pdlService
    ),
    val pdlAktorKafkaHwmOperations: KafkaHwmOperations = KafkaHwmService(
        kafkaConsumerConfig = applicationConfig.pdlAktorConsumer,
        hwmRepository = hwmRepository
    ),
    val pdlAktorKafkaConsumerService: PdlAktorKafkaConsumerService = PdlAktorKafkaConsumerService(
        kafkaConsumerConfig = applicationConfig.pdlAktorConsumer,
        kafkaKeysIdentitetRepository = kafkaKeysIdentitetRepository,
        hwmOperations = pdlAktorKafkaHwmOperations,
        identitetService = identitetService
    ),
    val pawHendelseKafkaConsumerService: PawHendelseKafkaConsumerService = PawHendelseKafkaConsumerService(
        meterRegistry = meterRegistry,
        kafkaKeysIdentitetRepository = kafkaKeysIdentitetRepository,
        kafkaKeysRepository = kafkaKeysRepository,
        kafkaKeysAuditRepository = kafkaKeysAuditRepository
    )
) {
    fun hentEllerOpprett(identitetsnummer: String): Either<Failure, ArbeidssoekerId> = runBlocking {
        kafkaKeysService.hentEllerOpprett(
            callId = CallId(UUID.randomUUID().toString()),
            identitet = Identitetsnummer(identitetsnummer)
        )
    }

    fun setUp() {
        Database.connect(dataSource)
        Flyway.configure()
            .dataSource(dataSource)
            .baselineOnMigrate(true)
            .load()
            .migrate()
        initSql.forEach { it.runAsSql() }
    }

    fun tearDown() {
        dataSource.connection.close()
    }

    companion object {
        fun buildWithPostgres(): TestContext = TestContext(
            dataSource = buildPostgresDataSource(),
            initSql = listOf("DROP SCHEMA public CASCADE", "CREATE SCHEMA public")
        )

        fun pdlClient(): PdlClient = PdlClient(
            url = "http://mock",
            tema = "tema",
            HttpClient(MockEngine {
                genererResponse(it)
            })
        ) { "fake token" }
    }
}
