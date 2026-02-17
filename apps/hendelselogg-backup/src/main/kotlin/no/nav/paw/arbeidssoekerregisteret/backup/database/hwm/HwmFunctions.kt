package no.nav.paw.arbeidssoekerregisteret.backup.database.hwm

import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update

fun initHwm(consumerVersion: Int, partitionCount: Int) =
    transaction {
        (0 until partitionCount)
            .filter { getHwm(consumerVersion, it) == null }
            .forEach { insertHwm(consumerVersion, it, -1) }
    }

fun getHwm(consumerVersion: Int, partition: Int): Long? =
    transaction {
        HwmTable
            .selectAll()
            .where { (HwmTable.partition eq partition) and (HwmTable.version eq consumerVersion) }
            .singleOrNull()?.get(HwmTable.offset)
    }


fun getAllHwms(consumerVersion: Int): List<Hwm> =
    transaction {
        HwmTable
            .selectAll()
            .where { HwmTable.version eq consumerVersion }
            .map {
                Hwm(
                    partition = it[HwmTable.partition],
                    offset = it[HwmTable.offset]
                )
            }
    }

fun insertHwm(consumerVersion: Int, partition: Int, offset: Long) =
    transaction {
        HwmTable.insert {
            it[HwmTable.version] = consumerVersion
            it[HwmTable.partition] = partition
            it[HwmTable.offset] = offset
        }
    }

fun updateHwm(consumerVersion: Int, partition: Int, offset: Long): Boolean =
    transaction {
        HwmTable.update({
            (HwmTable.partition eq partition) and
                    (HwmTable.offset less offset) and
                    (HwmTable.version eq consumerVersion)
        }) { it[HwmTable.offset] = offset } == 1
    }
