package no.nav.paw.arbeidssoekerregisteret.backup.database

import no.nav.paw.arbeidssoekerregisteret.backup.vo.Hwm
import org.jetbrains.exposed.sql.*

data class TransactionContext(
    val consumerVersion: Int,
    val transaction: Transaction
)

fun txContext(consumerVersion: Int): Transaction.() -> TransactionContext = {
    TransactionContext(consumerVersion, this)
}

fun TransactionContext.initHwm(partitionCount: Int) {
    (0 until partitionCount)
        .filter { getHwm(it) == null }
        .forEach { insertHwm(it, -1) }
}

fun TransactionContext.getHwm(partition: Int): Long? =
    HwmTable
        .selectAll()
        .where { (HwmTable.partition eq partition) and (HwmTable.version eq consumerVersion) }
        .singleOrNull()?.get(HwmTable.offset)

fun TransactionContext.getAllHwms(): List<Hwm> =
    HwmTable
        .selectAll()
        .where { HwmTable.version eq consumerVersion }
        .map {
            Hwm(
                partition = it[HwmTable.partition],
                offset = it[HwmTable.offset]
            )
        }

fun TransactionContext.insertHwm(partition: Int, offset: Long) {
    HwmTable.insert {
        it[HwmTable.version] = consumerVersion
        it[HwmTable.partition] = partition
        it[HwmTable.offset] = offset
    }
}

fun TransactionContext.updateHwm(partition: Int, offset: Long): Boolean =
    HwmTable
        .update({
            (HwmTable.partition eq partition) and
                    (HwmTable.offset less offset) and
                    (HwmTable.version eq consumerVersion)
        }
        ) { it[HwmTable.offset] = offset } == 1