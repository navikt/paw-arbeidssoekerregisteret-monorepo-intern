package no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.context

import com.fasterxml.jackson.databind.ObjectMapper
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.confluent.kafka.schemaregistry.testutil.MockSchemaRegistry
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.applogic.STATE_STORE_NAME
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.applogic.VarselHendelseJsonSerde
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.applogic.bekreftelseKafkaTopology
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.applogic.varselHendelserKafkaTopology
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.applogic.varselbygger.VarselMeldingBygger
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.config.KafkaTopologyConfig
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.config.MIN_SIDE_VARSEL_CONFIG
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.config.MinSideVarselConfig
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.model.VarselHendelse
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.repository.PeriodeRepository
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.repository.VarselRepository
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.service.VarselService
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.vo.InternTilstand
import no.nav.paw.arbeidssoekerregisteret.testdata.KafkaKeyContext
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelseSerde
import no.nav.paw.config.env.Local
import no.nav.paw.config.env.RuntimeEnvironment
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.kafkakeygenerator.client.inMemoryKafkaKeysMock
import no.nav.paw.serialization.jackson.buildObjectMapper
import no.nav.paw.serialization.kafka.buildJacksonSerde
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.utils.Time
import org.apache.kafka.streams.*
import org.apache.kafka.streams.state.KeyValueStore
import org.apache.kafka.streams.state.internals.InMemoryKeyValueBytesStoreSupplier
import org.apache.kafka.streams.state.internals.KeyValueStoreBuilder
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Period
import java.util.*
import javax.sql.DataSource

data class TestContext(
    val logger: Logger = LoggerFactory.getLogger("test.logger"),
    val objectMapper: ObjectMapper = buildObjectMapper,
    val dataSource: DataSource,
    val periodeRepository: PeriodeRepository,
    val varselRepository: VarselRepository,
    val varselService: VarselService,
    val periodeTopic: TestInputTopic<Long, Periode>,
    val bekreftelseHendelseTopic: TestInputTopic<Long, BekreftelseHendelse>,
    val tmsVarselTopic: TestOutputTopic<String, String>,
    val tmsVarselHendelseTopic: TestInputTopic<String, VarselHendelse>,
    val kevValueStore: KeyValueStore<Long, Period>,
    val kafkaKeyContext: KafkaKeyContext
) {
    operator fun <K, V> KeyValue<K, V>.component1(): K = key
    operator fun <K, V> KeyValue<K, V>.component2(): V = value

    fun initDatabase() {
        Flyway.configure()
            .dataSource(dataSource)
            .baselineOnMigrate(true)
            .load()
            .migrate()
        Database.connect(dataSource)
    }

    companion object {
        fun build(
            runtimeEnvironment: RuntimeEnvironment = Local,
            varselMeldingBygger: VarselMeldingBygger = VarselMeldingBygger(
                runtimeEnvironment = runtimeEnvironment,
                minSideVarselConfig = loadNaisOrLocalConfiguration<MinSideVarselConfig>(MIN_SIDE_VARSEL_CONFIG)
            ),
            kafkaKeyContext: KafkaKeyContext = KafkaKeyContext(inMemoryKafkaKeysMock()),
            kafkaTopologyConfig: KafkaTopologyConfig = KafkaTopologyConfig(
                bekreftelseStreamSuffix = "bekreftelse-v1",
                varselHendelseStreamSuffix = "varsel-hendelse-v1",
                periodeTopic = "periode-topic",
                bekreftelseHendelseTopic = "bekreftelse-hendelse-topic",
                tmsVarselTopic = "tms-oppgave-topic",
                tmsVarselHendelseTopic = "tms-varsel-hendelse-topic",
                skalSendeVarsler = true
            )
        ): TestContext {
            val dataSource = buildHikariTestDataSource()
            val meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
            val periodeRepository = PeriodeRepository()
            val varselRepository = VarselRepository()
            val varselService = VarselService(periodeRepository, varselRepository, varselMeldingBygger)
            val bekreftelseTopology = StreamsBuilder()
                .inMemStateStore()
                .bekreftelseKafkaTopology(
                    kafkaTopicsConfig = kafkaTopologyConfig,
                    varselService = varselService,
                    varselMeldingBygger = varselMeldingBygger
                )
                .varselHendelserKafkaTopology(
                    runtimeEnvironment = runtimeEnvironment,
                    kafkaTopicsConfig = kafkaTopologyConfig,
                    meterRegistry = meterRegistry,
                    varselService = varselService
                )
                .build()
            val varselTopology = StreamsBuilder()
                .varselHendelserKafkaTopology(
                    runtimeEnvironment = runtimeEnvironment,
                    kafkaTopicsConfig = kafkaTopologyConfig,
                    meterRegistry = meterRegistry,
                    varselService = varselService
                )
                .build()

            val bekreftelseTopologyTestDriver = TopologyTestDriver(bekreftelseTopology, kafkaStreamProperties)
            val varselTopologyTestDriver = TopologyTestDriver(varselTopology, kafkaStreamProperties)

            val periodeInputTopic = bekreftelseTopologyTestDriver.createInputTopic(
                kafkaTopologyConfig.periodeTopic,
                Serdes.Long().serializer(),
                createAvroSerde<Periode>().serializer()
            )
            val bekreftelseHendelseTopic = bekreftelseTopologyTestDriver.createInputTopic(
                kafkaTopologyConfig.bekreftelseHendelseTopic,
                Serdes.Long().serializer(),
                BekreftelseHendelseSerde().serializer(),
            )
            val tmsVarselTopic = bekreftelseTopologyTestDriver.createOutputTopic(
                kafkaTopologyConfig.tmsVarselTopic,
                Serdes.String().deserializer(),
                Serdes.String().deserializer()
            )
            val tmsVarselHendelseTopic = varselTopologyTestDriver.createInputTopic(
                kafkaTopologyConfig.tmsVarselHendelseTopic,
                Serdes.String().serializer(),
                VarselHendelseJsonSerde().serializer()
            )
            return TestContext(
                dataSource = dataSource,
                periodeRepository = periodeRepository,
                varselRepository = varselRepository,
                varselService = varselService,
                periodeTopic = periodeInputTopic,
                bekreftelseHendelseTopic = bekreftelseHendelseTopic,
                tmsVarselTopic = tmsVarselTopic,
                tmsVarselHendelseTopic = tmsVarselHendelseTopic,
                kevValueStore = bekreftelseTopologyTestDriver.getKeyValueStore(STATE_STORE_NAME.value),
                kafkaKeyContext = kafkaKeyContext
            )
        }

        private val kafkaStreamProperties = Properties().apply {
            this[StreamsConfig.APPLICATION_ID_CONFIG] = "test"
            this[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = "dummy:1234"
            this[StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG] = Serdes.Long().javaClass
            this[StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG] = SpecificAvroSerde<SpecificRecord>().javaClass
            this[KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS] = "true"
            this[KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG] = "mock://mock"
        }

        private inline fun <reified T : SpecificRecord> createAvroSerde(): Serde<T> {

            return SpecificAvroSerde<T>(MockSchemaRegistry.getClientForScope("mock")).apply {
                configure(
                    mapOf(
                        KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS to "true",
                        KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG to "mock://mock"
                    ),
                    false
                )
            }
        }

        private fun StreamsBuilder.inMemStateStore(): StreamsBuilder {
            addStateStore(
                KeyValueStoreBuilder(
                    InMemoryKeyValueBytesStoreSupplier(STATE_STORE_NAME.value),
                    Serdes.UUID(),
                    buildJacksonSerde<InternTilstand>(),
                    Time.SYSTEM
                )
            )
            return this
        }

        private fun buildHikariTestDataSource(): HikariDataSource = HikariDataSource(
            HikariConfig().apply {
                jdbcUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"
                driverClassName = "org.h2.Driver"
                maximumPoolSize = 3
                isAutoCommit = true
                connectionTimeout = Duration.ofSeconds(5).toMillis()
                idleTimeout = Duration.ofMinutes(5).toMillis()
                maxLifetime = Duration.ofMinutes(10).toMillis()
            }
        )
    }
}
