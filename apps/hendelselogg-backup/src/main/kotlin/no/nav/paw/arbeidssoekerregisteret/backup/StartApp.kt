package no.nav.paw.arbeidssoekerregisteret.backup

import io.ktor.server.application.Application
import io.ktor.server.engine.addShutdownHook
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics
import no.nav.paw.arbeidssoekerregisteret.backup.database.txContext
import no.nav.paw.arbeidssoekerregisteret.backup.database.updateHwm
import no.nav.paw.arbeidssoekerregisteret.backup.database.writeRecord
import no.nav.paw.arbeidssoekerregisteret.backup.vo.ApplicationContext
import no.nav.paw.arbeidssokerregisteret.intern.v1.Aarsak
import no.nav.paw.arbeidssokerregisteret.intern.v1.Avsluttet
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseSerializer
import no.nav.paw.config.env.appNameOrDefaultForLocal
import no.nav.paw.error.plugin.installErrorHandlingPlugin
import no.nav.paw.kafka.consumer.asSequence
import no.nav.paw.logging.logger.buildApplicationLogger
import no.nav.paw.logging.plugin.installLoggingPlugin
import no.nav.paw.security.authentication.plugin.installAuthenticationPlugin
import no.nav.paw.serialization.plugin.installContentNegotiationPlugin
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.Duration.ofMillis
import kotlin.system.exitProcess

fun main() {
    val logger = buildApplicationLogger

    val applicationContext = ApplicationContext.create()
    val appName = applicationContext.serverConfig.runtimeEnvironment.appNameOrDefaultForLocal()

    with(applicationContext.serverConfig) {
        logger.info("Starter $appName med hostname $host og port $port")

        embeddedServer(factory = Netty, port = port) {

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
        installErrorHandlingPlugin() // TODO: legg inn problemdetails i openapi spec
        installAuthenticationPlugin(securityConfig.authProviders)
        // TODO: Fortsett herifra
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


fun ApplicationContext.runApplication(
    hendelseSerializer: HendelseSerializer,
    source: Sequence<Iterable<ConsumerRecord<Long, Hendelse>>>
) {
    val counterInclude = meterRegistry.counter(RECORD_COUNTER, listOf(Tag.of("include", "true")))
    val counterExclude = meterRegistry.counter(RECORD_COUNTER, listOf(Tag.of("include", "false")))

    val kalkulertAarsakCounters = Aarsak.entries.associateWith { aarsak ->
        meterRegistry.counter(
            KALKULERT_AVSLUTTET_AARSAK,
            listOf(Tag.of("kalkulert_aarsak", aarsak.name))
        )
    }
    source.forEach { records ->
        transaction {
            with(txContext(this@runApplication)()) {
                records.forEach {
                    if (updateHwm(it.partition(), it.offset())) {
                        writeRecord(hendelseSerializer, it)
                        counterInclude.increment()

                        val hendelse = it.value()
                        if (hendelse is Avsluttet) {
                            kalkulertAarsakCounters[hendelse.kalkulertAarsak]?.increment()
                        }
                    } else {
                        counterExclude.increment()
                    }
                }
            }
        }
    }
}
