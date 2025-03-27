package no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup

import io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.brukerstoette.BrukerstoetteService
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.kafka.ConsumerHandler
import org.slf4j.LoggerFactory
import java.time.Duration
import kotlin.system.exitProcess

fun main() {
    val mainLogger = LoggerFactory.getLogger("app")
    runCatching {
        val applicationContext = initApplication()
        with(applicationContext) {
            ConsumerHandler(this).use { consumerHandler ->
                val hendelseListener = consumerHandler.getHendelseRebalanceListener()
                val bekreftelseListener = consumerHandler.getBekreftelseRebalanceListener()
                val paaVegneAvListener = consumerHandler.getPaaVegneAvRebalanceListener()

                meterRegistry.gauge("$ACTIVE_PARTITIONS_GAUGE-hendelse", hendelseListener) {
                    it.currentlyAssignedPartitions.size.toDouble()
                }
                meterRegistry.gauge("$ACTIVE_PARTITIONS_GAUGE-bekreftelse", bekreftelseListener) {
                    it.currentlyAssignedPartitions.size.toDouble()
                }
                meterRegistry.gauge("$ACTIVE_PARTITIONS_GAUGE-paaVegneAv", paaVegneAvListener) {
                    it.currentlyAssignedPartitions.size.toDouble()
                }

                val kafkaMetrics = listOf(
                    KafkaClientMetrics(hendelseConsumer),
                    KafkaClientMetrics(bekreftelseConsumer),
                    KafkaClientMetrics(paaVegneAvConsumer)
                )

                val service = BrukerstoetteService(
                    applicationContext = applicationContext,
                    kafkaKeysClient = kafkaKeysClient,
                )

                initKtor(meterRegistry, kafkaMetrics, azureConfig, service)

                val consumerFuture = consumerHandler.startConsumerTasks(pollTimeout = Duration.ofMillis(100),)

                consumerFuture.exceptionally { exception ->
                    mainLogger.error("En consumer feilet, avslutter app", exception)
                    exitProcess(1)
                }.join()
            }
        }
    }
    .onFailure {
        mainLogger.error("Application terminated due to an error", it)
        exitProcess(1)
    }
    .onSuccess {
        mainLogger.info("Application ended normally")
    }
}
