package no.nav.paw.arbeidssokerregisteret.backup.database

import no.nav.paw.arbeidssokerregisteret.backup.vo.Hwm
import org.jetbrains.exposed.sql.*

fun Transaction.initHwm(partitionCount: Int) {
    (0 until partitionCount)
        .filter { getHwm(it) == null }
        .forEach { insertHwm(it, -1) }
}

fun Transaction.getHwm(partition: Int): Long? =
    HwmTable
        .selectAll()
        .where { HwmTable.partition eq partition }
        .singleOrNull()?.get(HwmTable.offset)

fun Transaction.getAllHwms(): List<Hwm> =
    HwmTable
        .selectAll()
        .map {
            Hwm(
                partition = it[HwmTable.partition],
                offset = it[HwmTable.offset]
            )
        }

fun Transaction.insertHwm(partition: Int, offset: Long) {
    HwmTable.insert {
        it[HwmTable.partition] = partition
        it[HwmTable.offset] = offset
    }
}

fun Transaction.updateHwm(partition: Int, offset: Long): Boolean =
    HwmTable
        .update({ (HwmTable.partition eq partition) and (HwmTable.offset less offset) }) {
            it[HwmTable.offset] = offset
        } == 1