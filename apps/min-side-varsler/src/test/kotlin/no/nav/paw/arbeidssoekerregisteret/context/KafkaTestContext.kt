package no.nav.paw.arbeidssoekerregisteret.context

import io.confluent.kafka.schemaregistry.testutil.MockSchemaRegistry
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.config.ServerConfig
import no.nav.paw.arbeidssoekerregisteret.model.VarselHendelse
import no.nav.paw.arbeidssoekerregisteret.repository.BestillingRepository
import no.nav.paw.arbeidssoekerregisteret.repository.BestiltVarselRepository
import no.nav.paw.arbeidssoekerregisteret.repository.EksterntVarselRepository
import no.nav.paw.arbeidssoekerregisteret.repository.PeriodeRepository
import no.nav.paw.arbeidssoekerregisteret.repository.VarselRepository
import no.nav.paw.arbeidssoekerregisteret.service.BestillingService
import no.nav.paw.arbeidssoekerregisteret.service.VarselService
import no.nav.paw.arbeidssoekerregisteret.topology.bekreftelseKafkaTopology
import no.nav.paw.arbeidssoekerregisteret.topology.periodeKafkaTopology
import no.nav.paw.arbeidssoekerregisteret.topology.varselHendelserKafkaTopology
import no.nav.paw.arbeidssoekerregisteret.utils.VarselHendelseJsonSerde
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelseSerde
import no.nav.paw.security.authentication.config.SecurityConfig
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.TestInputTopic
import org.apache.kafka.streams.TestOutputTopic
import org.apache.kafka.streams.TopologyTestDriver
import java.util.*
import javax.sql.DataSource
import kotlin.collections.set

class KafkaTestContext(
    override val dataSource: DataSource,
    override val resetDatabaseSql: List<String>,
    override val mockOAuth2Server: MockOAuth2Server,
    override val serverConfig: ServerConfig,
    override val applicationConfig: ApplicationConfig,
    override val securityConfig: SecurityConfig,
    override val prometheusMeterRegistry: PrometheusMeterRegistry,
    override val periodeRepository: PeriodeRepository,
    override val varselRepository: VarselRepository,
    override val eksternVarselRepository: EksterntVarselRepository,
    override val bestillingRepository: BestillingRepository,
    override val bestiltVarselRepository: BestiltVarselRepository,
    override val varselService: VarselService,
    override val bestillingService: BestillingService,
    val periodeTopic: TestInputTopic<Long, Periode>,
    val periodeVarselTopic: TestOutputTopic<String, String>,
    val bekreftelsePeriodeTopic: TestInputTopic<Long, Periode>,
    val bekreftelseHendelseTopic: TestInputTopic<Long, BekreftelseHendelse>,
    val bekreftelseVarselTopic: TestOutputTopic<String, String>,
    val varselHendelseTopic: TestInputTopic<String, VarselHendelse>
) : TestContext(
    dataSource = dataSource,
    resetDatabaseSql = resetDatabaseSql,
    mockOAuth2Server = mockOAuth2Server,
    serverConfig = serverConfig,
    applicationConfig = applicationConfig,
    securityConfig = securityConfig,
    prometheusMeterRegistry = prometheusMeterRegistry,
    periodeRepository = periodeRepository,
    varselRepository = varselRepository,
    eksternVarselRepository = eksternVarselRepository,
    bestillingRepository = bestillingRepository,
    bestiltVarselRepository = bestiltVarselRepository,
    bestillingService = bestillingService,
    varselService = varselService
) {
    operator fun <K, V> KeyValue<K, V>.component1(): K = key
    operator fun <K, V> KeyValue<K, V>.component2(): V = value

    companion object {
        fun buildWithH2(): KafkaTestContext =
            build(buildH2DataSource(), listOf("DROP ALL OBJECTS"))

        fun buildWithPostgres(): KafkaTestContext =
            build(buildPostgresDataSource(), listOf("DROP SCHEMA public CASCADE", "CREATE SCHEMA public"))

        private fun build(dataSource: DataSource, resetDatabaseSql: List<String>): KafkaTestContext {
            with(TestContext.build(dataSource, resetDatabaseSql)) {
                val periodeTopology = StreamsBuilder()
                    .periodeKafkaTopology(
                        runtimeEnvironment = serverConfig.runtimeEnvironment,
                        applicationConfig = applicationConfig,
                        meterRegistry = prometheusMeterRegistry,
                        varselService = varselService
                    )
                    .build()
                val bekreftelseTopology = StreamsBuilder()
                    .bekreftelseKafkaTopology(
                        runtimeEnvironment = serverConfig.runtimeEnvironment,
                        applicationConfig = applicationConfig,
                        meterRegistry = prometheusMeterRegistry,
                        varselService = varselService
                    )
                    .build()
                val varselTopology = StreamsBuilder()
                    .varselHendelserKafkaTopology(
                        runtimeEnvironment = serverConfig.runtimeEnvironment,
                        applicationConfig = applicationConfig,
                        meterRegistry = prometheusMeterRegistry,
                        varselService = varselService
                    )
                    .build()

                val periodeTopologyTestDriver = TopologyTestDriver(periodeTopology, kafkaStreamProperties)
                val bekreftelseTopologyTestDriver = TopologyTestDriver(bekreftelseTopology, kafkaStreamProperties)
                val varselTopologyTestDriver = TopologyTestDriver(varselTopology, kafkaStreamProperties)

                val periodeInputTopic = periodeTopologyTestDriver.createInputTopic(
                    applicationConfig.periodeTopic,
                    Serdes.Long().serializer(),
                    createAvroSerde<Periode>().serializer()
                )
                val periodeVarselTopic = periodeTopologyTestDriver.createOutputTopic(
                    applicationConfig.tmsVarselTopic,
                    Serdes.String().deserializer(),
                    Serdes.String().deserializer()
                )
                val bekreftelsePeriodeTopic = bekreftelseTopologyTestDriver.createInputTopic(
                    applicationConfig.periodeTopic,
                    Serdes.Long().serializer(),
                    createAvroSerde<Periode>().serializer()
                )
                val bekreftelseHendelseTopic = bekreftelseTopologyTestDriver.createInputTopic(
                    applicationConfig.bekreftelseHendelseTopic,
                    Serdes.Long().serializer(),
                    BekreftelseHendelseSerde().serializer()
                )
                val bekreftelseVarselTopic = bekreftelseTopologyTestDriver.createOutputTopic(
                    applicationConfig.tmsVarselTopic,
                    Serdes.String().deserializer(),
                    Serdes.String().deserializer()
                )
                val varselHendelseTopic = varselTopologyTestDriver.createInputTopic(
                    applicationConfig.tmsVarselHendelseTopic,
                    Serdes.String().serializer(),
                    VarselHendelseJsonSerde().serializer()
                )

                return KafkaTestContext(
                    mockOAuth2Server = mockOAuth2Server,
                    dataSource = dataSource,
                    resetDatabaseSql = resetDatabaseSql,
                    serverConfig = serverConfig,
                    applicationConfig = applicationConfig,
                    securityConfig = securityConfig,
                    prometheusMeterRegistry = prometheusMeterRegistry,
                    periodeRepository = periodeRepository,
                    varselRepository = varselRepository,
                    eksternVarselRepository = eksternVarselRepository,
                    bestillingRepository = bestillingRepository,
                    bestiltVarselRepository = bestiltVarselRepository,
                    varselService = varselService,
                    bestillingService = bestillingService,
                    periodeTopic = periodeInputTopic,
                    periodeVarselTopic = periodeVarselTopic,
                    bekreftelsePeriodeTopic = bekreftelsePeriodeTopic,
                    bekreftelseHendelseTopic = bekreftelseHendelseTopic,
                    bekreftelseVarselTopic = bekreftelseVarselTopic,
                    varselHendelseTopic = varselHendelseTopic
                )
            }
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
    }
}
