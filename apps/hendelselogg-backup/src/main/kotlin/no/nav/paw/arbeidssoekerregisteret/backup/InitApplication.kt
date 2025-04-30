package no.nav.paw.arbeidssoekerregisteret.backup

import io.micrometer.core.instrument.Tag
import no.nav.paw.arbeidssoekerregisteret.backup.database.*
import no.nav.paw.arbeidssoekerregisteret.backup.vo.ApplicationContext
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

fun initApplication(context: ApplicationContext) {
    val logger = LoggerFactory.getLogger("backup-init")
    logger.info("Initializing application...")
    val partitions = context.hendelseKafkaConsumer.partitionsFor(context.applicationConfig.hendelsesloggTopic).count()
    fun Transaction.txContext(): TransactionContext = TransactionContext(context.applicationConfig.version, this)
    val allHwms = transaction {
        with(txContext()) {
            initHwm(partitions)
            getAllHwms()
        }
    }
    allHwms.forEach { hwm ->
        context.prometheusMeterRegistry.gauge(Metrics.HWM_GAUGE, listOf(Tag.of("partition", hwm.partition.toString())), context) { _ ->
            transaction {
                txContext().getHwm(hwm.partition)?.toDouble() ?: -1.0
            }
        }
    }
    logger.info("Application initialized")
}
