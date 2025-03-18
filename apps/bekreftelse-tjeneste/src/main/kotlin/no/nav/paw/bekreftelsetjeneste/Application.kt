package no.nav.paw.bekreftelsetjeneste

import io.ktor.server.application.Application
import io.ktor.server.engine.addShutdownHook
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.bekreftelsetjeneste.paavegneav.BekreftelsePaaVegneAvSerde
import no.nav.paw.bekreftelsetjeneste.config.APPLICATION_CONFIG_FILE_NAME
import no.nav.paw.bekreftelsetjeneste.config.ApplicationConfig
import no.nav.paw.bekreftelsetjeneste.config.BEKREFTELSE_CONFIG_FILE_NAME
import no.nav.paw.bekreftelsetjeneste.config.BekreftelseKonfigurasjon
import no.nav.paw.bekreftelsetjeneste.config.SERVER_CONFIG_FILE_NAME
import no.nav.paw.bekreftelsetjeneste.config.ServerConfig
import no.nav.paw.bekreftelsetjeneste.config.StaticConfigValues
import no.nav.paw.bekreftelsetjeneste.context.ApplicationContext
import no.nav.paw.bekreftelsetjeneste.metrics.TilstandsGauge
import no.nav.paw.bekreftelsetjeneste.plugins.buildKafkaStreams
import no.nav.paw.bekreftelsetjeneste.plugins.configureKafka
import no.nav.paw.bekreftelsetjeneste.plugins.configureMetrics
import no.nav.paw.bekreftelsetjeneste.routes.metricsRoutes
import no.nav.paw.bekreftelsetjeneste.startdatohaandtering.OddetallPartallMap
import no.nav.paw.bekreftelsetjeneste.startdatohaandtering.StatiskMapOddetallPartallMap
import no.nav.paw.bekreftelsetjeneste.startdatohaandtering.finnFiler
import no.nav.paw.bekreftelsetjeneste.startdatohaandtering.oddetallPartallMapFraCsvFil
import no.nav.paw.bekreftelsetjeneste.tilstand.InternTilstandSerde
import no.nav.paw.bekreftelsetjeneste.topology.buildTopology
import no.nav.paw.config.env.appNameOrDefaultForLocal
import no.nav.paw.config.env.currentRuntimeEnvironment
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.health.route.healthRoutes
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.state.Stores
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean

val logger = LoggerFactory.getLogger("bekreftelse.tjeneste.application")
fun main() {
    val serverConfig = loadNaisOrLocalConfiguration<ServerConfig>(SERVER_CONFIG_FILE_NAME)
    val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>(APPLICATION_CONFIG_FILE_NAME)
    val bekreftelseKonfigurasjon = loadNaisOrLocalConfiguration<BekreftelseKonfigurasjon>(BEKREFTELSE_CONFIG_FILE_NAME)

    logger.info("Starter: ${currentRuntimeEnvironment.appNameOrDefaultForLocal()}")
    val keepGoing = AtomicBoolean(true)
    val syncFiler = finnFiler(StaticConfigValues.syncMappe)
    logger.info("Følgende filer ble funnet: $syncFiler")
    val oddetallPartallMap = oddetallPartallMapFraCsvFil(
        header = false,
        filer = syncFiler,
        delimiter = ";",
        identitetsnummerKolonne = 0,
        ukenummerKolonne = 1,
        partall = "P",
        oddetall = "O"
    )
    with(serverConfig) {
        embeddedServer(Netty, port = port) {
            module(
                applicationConfig = applicationConfig,
                bekreftelseKonfigurasjon = bekreftelseKonfigurasjon,
                oddetallPartallMap = oddetallPartallMap
            )
        }.apply {
            addShutdownHook {
                keepGoing.set(false)
                stop(gracePeriodMillis, timeoutMillis)
            }
            start(wait = true)
        }
    }
}

fun Application.module(
    applicationConfig: ApplicationConfig,
    bekreftelseKonfigurasjon: BekreftelseKonfigurasjon,
    keepGoing: AtomicBoolean = AtomicBoolean(true),
    oddetallPartallMap: OddetallPartallMap
) {
    val applicationContext = ApplicationContext.create(
        applicationConfig = applicationConfig,
        bekreftelseKonfigurasjon = bekreftelseKonfigurasjon,
        oddetallPartallMap = oddetallPartallMap
    )
    val stream = StreamsBuilder()
    stream.addStateStore(
        Stores.keyValueStoreBuilder(
            Stores.persistentKeyValueStore(applicationContext.applicationConfig.kafkaTopology.internStateStoreName),
            Serdes.UUID(),
            InternTilstandSerde()
        )
    )
    stream.addStateStore(
        Stores.keyValueStoreBuilder(
            Stores.persistentKeyValueStore(applicationContext.applicationConfig.kafkaTopology.bekreftelsePaaVegneAvStateStoreName),
            Serdes.UUID(),
            BekreftelsePaaVegneAvSerde()
        )
    )
    val kafkaTopology = stream.buildTopology(applicationContext)
    val kafkaStreams = buildKafkaStreams(applicationContext, kafkaTopology)
    registerStreamStateGauge(applicationContext.prometheusMeterRegistry, kafkaStreams)
    TilstandsGauge(
        kafkaStreams = kafkaStreams,
        paaVegneAvStoreName = applicationContext.applicationConfig.kafkaTopology.bekreftelsePaaVegneAvStateStoreName,
        tilstandStoreName = applicationContext.applicationConfig.kafkaTopology.internStateStoreName,
        keepGoing = keepGoing,
        prometheusMeterRegistry = applicationContext.prometheusMeterRegistry,
        bekreftelseKonfigurasjon = applicationContext.bekreftelseKonfigurasjon
    )
        .stateGaugeTask
        .handle{ _, ex ->
            if (ex != null) {
                logger.error("Metrics oppdateringer er avsluttet med feil", ex)
            } else {
                logger.info("Metrics oppdateringer er avsluttet")
            }
        }

    configureMetrics(applicationContext)
    configureKafka(applicationContext, kafkaStreams)

    routing {
        healthRoutes(applicationContext.healthIndicatorRepository)
        metricsRoutes(applicationContext)
    }
}

private fun registerStreamStateGauge(
    prometheusMeterRegistry: PrometheusMeterRegistry,
    kafkaStreams: KafkaStreams
) {
    prometheusMeterRegistry.gauge(
        "paw_arbeidssokerregisteret_stream_state",
        listOf(),
        kafkaStreams
    ) {
        when (try {
            kafkaStreams.state()
        } catch (ex: TimeoutException) {
            -1
        }) {
            KafkaStreams.State.RUNNING -> 0
            KafkaStreams.State.CREATED -> 1
            KafkaStreams.State.REBALANCING -> 2
            KafkaStreams.State.PENDING_SHUTDOWN -> 3
            KafkaStreams.State.NOT_RUNNING -> 4
            KafkaStreams.State.ERROR -> -2
            else -> 6
        }.toDouble()
    }
}