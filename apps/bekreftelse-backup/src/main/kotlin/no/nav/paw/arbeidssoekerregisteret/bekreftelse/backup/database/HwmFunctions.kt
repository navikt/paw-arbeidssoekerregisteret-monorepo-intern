package no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.database

import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.vo.ApplicationContext
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.vo.Hwm
import org.jetbrains.exposed.sql.*

data class TransactionContext(
    val appContext: ApplicationContext,
    val transaction: Transaction
)

fun txContext(applicationContext: ApplicationContext): Transaction.() -> TransactionContext = {
    TransactionContext(applicationContext, this)
}

fun TransactionContext.initHwm(partitionCount: Int, topic: String) {
    (0 until partitionCount)
        .filter { getHwm(it, topic) == null }
        .forEach { insertHwm(it, -1, topic) }
}

fun TransactionContext.getHwm(partition: Int, topic: String): Long? =
    BekreftelseHwmTable
        .selectAll()
        .where { (BekreftelseHwmTable.partition eq partition) and (BekreftelseHwmTable.version eq appContext.consumerVersion) and (BekreftelseHwmTable.topic eq topic) }
        .singleOrNull()?.get(BekreftelseHwmTable.offset)

fun TransactionContext.getAllTopicHwms(topic: String): List<Hwm> =
    BekreftelseHwmTable
        .selectAll()
        .where { (BekreftelseHwmTable.version eq appContext.consumerVersion) and (BekreftelseHwmTable.topic eq topic) }
        .map {
            Hwm(
                partition = it[BekreftelseHwmTable.partition],
                offset = it[BekreftelseHwmTable.offset],
                topic = it[BekreftelseHwmTable.topic],
            )
        }
fun TransactionContext.getAllHwms(): List<Hwm> =
    BekreftelseHwmTable
        .selectAll()
        .where { BekreftelseHwmTable.version eq appContext.consumerVersion }
        .map {
            Hwm(
                partition = it[BekreftelseHwmTable.partition],
                offset = it[BekreftelseHwmTable.offset],
                topic = it[BekreftelseHwmTable.topic],
            )
        }

fun TransactionContext.insertHwm(partition: Int, offset: Long, topic: String) {
    BekreftelseHwmTable.insert {
        it[BekreftelseHwmTable.version] = appContext.consumerVersion
        it[BekreftelseHwmTable.partition] = partition
        it[BekreftelseHwmTable.offset] = offset
        it[BekreftelseHwmTable.topic] = topic
    }
}

fun TransactionContext.updateHwm(partition: Int, offset: Long, topic: String): Boolean =
    BekreftelseHwmTable
        .update({
            (BekreftelseHwmTable.partition eq partition) and
            (BekreftelseHwmTable.offset less offset) and
            (BekreftelseHwmTable.version eq appContext.consumerVersion) and
            (BekreftelseHwmTable.topic eq topic)
        }) { it[BekreftelseHwmTable.offset] = offset } == 1