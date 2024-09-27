package no.nav.paw.arbeidssoekerregisteret.backup.database

import no.nav.paw.arbeidssoekerregisteret.backup.vo.ApplicationContext
import no.nav.paw.arbeidssoekerregisteret.backup.vo.Hwm
import org.jetbrains.exposed.sql.*

data class TransactionContext(
    val appContext: ApplicationContext,
    val transaction: Transaction
)

fun txContext(applicationContext: ApplicationContext): Transaction.() -> TransactionContext = {
    TransactionContext(applicationContext, this)
}


fun TransactionContext.initHwm(partitionCount: Int) {
    (0 until partitionCount)
        .filter { getHwm(it) == null }
        .forEach { insertHwm(it, -1) }
}

fun TransactionContext.getHwm(partition: Int): Long? =
    HwmTable
        .selectAll()
        .where { (HwmTable.partition eq partition) and (HwmTable.version eq appContext.consumerVersion) }
        .singleOrNull()?.get(HwmTable.offset)

fun TransactionContext.getAllHwms(): List<Hwm> =
    HwmTable
        .selectAll()
        .where { HwmTable.version eq appContext.consumerVersion }
        .map {
            Hwm(
                partition = it[HwmTable.partition],
                offset = it[HwmTable.offset]
            )
        }

fun TransactionContext.insertHwm(partition: Int, offset: Long) {
    HwmTable.insert {
        it[HwmTable.version] = appContext.consumerVersion
        it[HwmTable.partition] = partition
        it[HwmTable.offset] = offset
    }
}

fun TransactionContext.updateHwm(partition: Int, offset: Long): Boolean =
    HwmTable
        .update({
            (HwmTable.partition eq partition) and
                    (HwmTable.offset less offset) and
                    (HwmTable.version eq appContext.consumerVersion)
        }
        ) { it[HwmTable.offset] = offset } == 1