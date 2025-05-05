package no.nav.paw.arbeidssoekerregisteret.backup

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.engine.addShutdownHook
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.routing
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics
import no.nav.paw.arbeidssoekerregisteret.backup.Metrics.Companion.KALKULERT_AVSLUTTET_AARSAK
import no.nav.paw.arbeidssoekerregisteret.backup.Metrics.Companion.RECORD_COUNTER
import no.nav.paw.arbeidssoekerregisteret.backup.database.updateHwm
import no.nav.paw.arbeidssoekerregisteret.backup.database.writeRecord
import no.nav.paw.arbeidssoekerregisteret.backup.health.HealthIndicatorConsumerExceptionHandler
import no.nav.paw.arbeidssoekerregisteret.backup.vo.ApplicationContext
import no.nav.paw.arbeidssokerregisteret.intern.v1.Aarsak
import no.nav.paw.arbeidssokerregisteret.intern.v1.Avsluttet
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseSerializer
import no.nav.paw.config.env.appNameOrDefaultForLocal
import no.nav.paw.database.plugin.installDatabasePlugin
import no.nav.paw.error.plugin.installErrorHandlingPlugin
import no.nav.paw.health.model.LivenessHealthIndicator
import no.nav.paw.health.model.ReadinessHealthIndicator
import no.nav.paw.kafka.consumer.NonCommittingKafkaConsumerWrapper
import no.nav.paw.kafka.consumer.asSequence
import no.nav.paw.kafka.plugin.KafkaConsumerPlugin
import no.nav.paw.logging.logger.buildApplicationLogger
import no.nav.paw.logging.plugin.installLoggingPlugin
import no.nav.paw.metrics.plugin.installWebAppMetricsPlugin
import no.nav.paw.security.authentication.plugin.installAuthenticationPlugin
import no.nav.paw.serialization.plugin.installContentNegotiationPlugin
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.Duration.ofMillis
import kotlin.system.exitProcess
import no.nav.paw.arbeidssoekerregisteret.backup.health.configureHealthRoutes
import no.nav.paw.config.env.ProdGcp
import no.nav.paw.config.env.currentRuntimeEnvironment
import no.nav.paw.kafka.listener.NoopConsumerRebalanceListener

fun main() {
    val logger = buildApplicationLogger

    val applicationContext = ApplicationContext.create()
    val appName = applicationContext.serverConfig.runtimeEnvironment.appNameOrDefaultForLocal()

    with(applicationContext.serverConfig) {
        logger.info("Starter $appName med hostname $host og port $port")

        embeddedServer(factory = Netty, port = port) {
            module(applicationContext)
        }.apply {
            addShutdownHook {
                logger.info("Avslutter $appName")
                stop(gracePeriodMillis, timeoutMillis)
            }
            start(wait = true)
        }
    }
}

fun Application.module(applicationContext: ApplicationContext) {
    with(applicationContext) {
        installLoggingPlugin()
        installContentNegotiationPlugin()
        installWebAppMetricsPlugin(
            meterRegistry = prometheusMeterRegistry,
            additionalMeterBinders = listOf(additionalMeterBinder)
        )
        installDatabasePlugin(dataSource)
        installKafkaPlugin(this, hendelseKafkaConsumer)
        installErrorHandlingPlugin()
        installAuthenticationPlugin(securityConfig.authProviders)
        routing {
            swaggerUI(path = "docs/brukerstoette", swaggerFile = "openapi/Brukerstoette.yaml")
            configureHealthRoutes(prometheusMeterRegistry)
            if (currentRuntimeEnvironment is ProdGcp) {
                authenticate("azure") {
                    configureBrukerstoetteRoutes(brukerstoetteService)
                }
            } else {
                configureBrukerstoetteRoutes(brukerstoetteService)
            }
        }
    }
}

fun Application.installKafkaPlugin(
    applicationContext: ApplicationContext,
    hendelseKafkaConsumer: KafkaConsumer<Long, Hendelse>,
) {
    install(KafkaConsumerPlugin<Long, Hendelse>("Hendelseslogg")) {
        this.onConsume = applicationContext::onConsume
        this.kafkaConsumerWrapper = NonCommittingKafkaConsumerWrapper(
            rebalanceListener = HwmRebalanceListener(context = applicationContext, consumer = hendelseKafkaConsumer),
            topics = listOf(applicationContext.applicationConfig.hendelsesloggTopic),
            consumer = hendelseKafkaConsumer,
            exceptionHandler = HealthIndicatorConsumerExceptionHandler(
                LivenessHealthIndicator(),
                ReadinessHealthIndicator()
            )
        )
    }
}

    fun ApplicationContext.onConsume(records: ConsumerRecords<Long, Hendelse>) {
        val counterInclude = prometheusMeterRegistry.counter(RECORD_COUNTER, listOf(Tag.of("include", "true")))
        val counterExclude = prometheusMeterRegistry.counter(RECORD_COUNTER, listOf(Tag.of("include", "false")))

        val kalkulertAarsakCounters = Aarsak.entries.associateWith { aarsak ->
            prometheusMeterRegistry.counter(
                KALKULERT_AVSLUTTET_AARSAK,
                listOf(Tag.of("kalkulert_aarsak", aarsak.name))
            )
        }

        transaction {
            records.asSequence().forEach { record ->
                if (updateHwm(applicationConfig.version, record.partition(), record.offset())) {
                    writeRecord(applicationConfig.version, HendelseSerializer(), record)
                    counterInclude.increment()

                    val hendelse = record.value()
                    if (hendelse is Avsluttet) {
                        kalkulertAarsakCounters[hendelse.kalkulertAarsak]?.increment()
                    }
                } else {
                    counterExclude.increment()
                }
            }
        }
    }


fun main2() {
    val mainLogger = LoggerFactory.getLogger("app")
    runCatching {
        val context = ApplicationContext.create()
        initApplication(context)
        with(context) {
            hendelseKafkaConsumer.use {
                val hwmRebalanceListener = HwmRebalanceListener(context = this, consumer = hendelseKafkaConsumer)
                prometheusMeterRegistry.gauge(
                    Metrics.ACTIVE_PARTITIONS_GAUGE,
                    hwmRebalanceListener
                ) { it.currentlyAssignedPartitions.size.toDouble() }
                hendelseKafkaConsumer.subscribe(listOf(applicationConfig.hendelsesloggTopic), hwmRebalanceListener)
                logger.info("Started subscription. Currently assigned partitions: ${hendelseKafkaConsumer.assignment()}")

                initKtor(
                    prometheusMeterRegistry = prometheusMeterRegistry,
                    binders = listOf(KafkaClientMetrics(hendelseKafkaConsumer)),
                    azureConfig = azureConfig,
                    brukerstoetteService = context.brukerstoetteService
                )
                runApplication(
                    hendelseSerializer = HendelseSerializer(),
                    source = hendelseKafkaConsumer.asSequence(
                        stop = shutdownCalled,
                        pollTimeout = ofMillis(500),
                        closeTimeout = ofMillis(100)
                    )
                )
            }
        }
    }
        .onFailure {
            mainLogger.error("Application terminated due to an error", it)
            exitProcess(1)
        }
        .onSuccess { mainLogger.info("Application ended normally") }
}
