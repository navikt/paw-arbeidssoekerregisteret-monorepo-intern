package no.nav.paw.arbeidssoekerregisteret.backup

import io.micrometer.core.instrument.Tag
import no.nav.paw.arbeidssoekerregisteret.backup.database.updateHwm
import no.nav.paw.arbeidssoekerregisteret.backup.database.writeRecord
import no.nav.paw.arbeidssoekerregisteret.backup.health.initHealthMonitoring
import no.nav.paw.arbeidssoekerregisteret.backup.vo.ApplicationContext
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseSerializer
import no.nav.paw.config.kafka.asSequence
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.Duration.ofMillis
import kotlin.system.exitProcess

fun main() {
    val mainLogger = LoggerFactory.getLogger("app")
    runCatching {
        val (consumer, applicationContext) = initApplication()
        with(applicationContext) {
            consumer.use {
                val hwmRebalanceListener = HwmRebalanceListener(context = this, consumer = consumer)
                meterRegistry.gauge(ACTIVE_PARTITIONS_GAUGE, hwmRebalanceListener) { it.currentlyAssignedPartitions.size.toDouble() }
                initHealthMonitoring(consumer, meterRegistry)
                consumer.subscribe(listOf(HENDELSE_TOPIC), hwmRebalanceListener)
                logger.info("Started subscription. Currently assigned partitions: ${consumer.assignment()}")
                with(HendelseSerializer()) {
                    runApplication(
                        source = consumer.asSequence(
                            stop = shutdownCalled,
                            pollTimeout = ofMillis(500),
                            closeTimeout = ofMillis(100)
                        )
                    )
                }
            }
        }
    }
        .onFailure {
            mainLogger.error("Application terminated due to an error", it)
            exitProcess(1)
        }
        .onSuccess { mainLogger.info("Application ended normally") }
}


context(ApplicationContext, HendelseSerializer)
fun runApplication(source: Sequence<Iterable<ConsumerRecord<Long, Hendelse>>>) {
    val counterInclude = meterRegistry.counter(RECORD_COUNTER, listOf(Tag.of("include", "true")))
    val counterExclude = meterRegistry.counter(RECORD_COUNTER, listOf(Tag.of("include", "false")))
    source.forEach { records ->
        transaction {
            records.forEach {
                if (updateHwm(it.partition(), it.offset())) {
                    writeRecord(it)
                    counterInclude.increment()
                } else {
                    counterExclude.increment()
                }
            }
        }
    }
}
