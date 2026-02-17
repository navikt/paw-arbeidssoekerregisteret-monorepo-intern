package no.nav.paw.arbeidssoekerregisteret.backup.database.hwm

import org.jetbrains.exposed.v1.core.Table

object HwmTable : Table("hwm") {
    val version = integer("version")
    val partition = integer("kafka_partition")
    val offset = long("kafka_offset")
    override val primaryKey: PrimaryKey = PrimaryKey(version, partition)
}