package no.nav.paw.arbeidssoekerregisteret.backup

import io.micrometer.core.instrument.Tag
import no.nav.paw.arbeidssoekerregisteret.backup.database.*
import no.nav.paw.arbeidssoekerregisteret.backup.vo.ApplicationContext
import no.nav.paw.arbeidssoekerregisteret.backup.vo.ApplicationContextOld
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import org.apache.kafka.clients.consumer.Consumer
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

const val CURRENT_VERSION = 1
const val HENDELSE_TOPIC = "paw.arbeidssoker-hendelseslogg-v1"
val CONSUMER_GROUP = "arbeidssoekerregisteret-backup-$CURRENT_VERSION"

fun initApplication(): Pair<Consumer<Long, Hendelse>, ApplicationContextOld> {
    val logger = LoggerFactory.getLogger("backup-init")
    logger.info("Initializing application...")
    //TODO, husk denne
    Runtime.getRuntime().addShutdownHook(Thread {
        AtomicBoolean(false).set(true)
    })
    val context = ApplicationContext.create()
    val partitions = context.hendelseKafkaConsumer.partitionsFor(HENDELSE_TOPIC).count()
    fun Transaction.txContext(): TransactionContext = TransactionContext(context, this)
    val allHwms = transaction {
        with(txContext()) {
            initHwm(partitions)
            getAllHwms()
        }
    }
    allHwms.forEach { hwm ->
        context.meterRegistry.gauge(Metrics.HWM_GAUGE, listOf(Tag.of("partition", hwm.partition.toString())), context) { _ ->
            transaction {
                txContext().getHwm(hwm.partition)?.toDouble() ?: -1.0
            }
        }
    }
    logger.info("Application initialized")
    return Pair(consumer, context)
}
