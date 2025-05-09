package no.nav.paw.arbeidssoekerregisteret.backup.database

import no.nav.paw.arbeidssoekerregisteret.backup.vo.Hwm
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

fun initHwm(consumerVersion: Int, partitionCount: Int) {
    transaction {
        (0 until partitionCount)
            .filter { getHwm(consumerVersion, it) == null }
            .forEach { insertHwm(consumerVersion, it, -1) }
    }
}

fun Transaction.getHwm(consumerVersion: Int, partition: Int): Long? =
    HwmTable
        .selectAll()
        .where { (HwmTable.partition eq partition) and (HwmTable.version eq consumerVersion) }
        .singleOrNull()?.get(HwmTable.offset)

fun Transaction.getAllHwms(consumerVersion: Int): List<Hwm> =
    HwmTable
        .selectAll()
        .where { HwmTable.version eq consumerVersion }
        .map {
            Hwm(
                partition = it[HwmTable.partition],
                offset = it[HwmTable.offset]
            )
        }

fun Transaction.insertHwm(consumerVersion: Int, partition: Int, offset: Long) {
    HwmTable.insert {
        it[HwmTable.version] = consumerVersion
        it[HwmTable.partition] = partition
        it[HwmTable.offset] = offset
    }
}

fun Transaction.updateHwm(consumerVersion: Int, partition: Int, offset: Long): Boolean =
    HwmTable
        .update({
            (HwmTable.partition eq partition) and
            (HwmTable.offset less offset) and
            (HwmTable.version eq consumerVersion)
        }) { it[HwmTable.offset] = offset } == 1