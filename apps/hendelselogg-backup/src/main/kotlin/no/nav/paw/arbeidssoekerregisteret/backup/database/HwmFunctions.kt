package no.nav.paw.arbeidssoekerregisteret.backup.database

import no.nav.paw.arbeidssoekerregisteret.backup.vo.ApplicationContext
import no.nav.paw.arbeidssoekerregisteret.backup.vo.Hwm
import org.jetbrains.exposed.sql.*

context(ApplicationContext)
fun Transaction.initHwm(partitionCount: Int) {
    (0 until partitionCount)
        .filter { getHwm(it) == null }
        .forEach { insertHwm(it, -1) }
}

context(ApplicationContext)
fun Transaction.getHwm(partition: Int): Long? =
    HwmTable
        .selectAll()
        .where { (HwmTable.partition eq partition) and (HwmTable.version eq consumerVersion) }
        .singleOrNull()?.get(HwmTable.offset)

context(ApplicationContext)
fun Transaction.getAllHwms(): List<Hwm> =
    HwmTable
        .selectAll()
        .where { HwmTable.version eq consumerVersion }
        .map {
            Hwm(
                partition = it[HwmTable.partition],
                offset = it[HwmTable.offset]
            )
        }

context(ApplicationContext)
fun Transaction.insertHwm(partition: Int, offset: Long) {
    HwmTable.insert {
        it[HwmTable.version] = consumerVersion
        it[HwmTable.partition] = partition
        it[HwmTable.offset] = offset
    }
}

context(ApplicationContext)
fun Transaction.updateHwm(partition: Int, offset: Long): Boolean =
    HwmTable
        .update({
            (HwmTable.partition eq partition) and
                    (HwmTable.offset less offset) and
                    (HwmTable.version eq consumerVersion)
        }
        ) { it[HwmTable.offset] = offset } == 1