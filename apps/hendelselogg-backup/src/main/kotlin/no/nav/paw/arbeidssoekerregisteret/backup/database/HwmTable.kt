package no.nav.paw.arbeidssoekerregisteret.backup.database

import org.jetbrains.exposed.sql.Table

object HwmTable: Table("hwm") {
    val version = integer("version")
    val partition = integer("kafka_partition")
    val offset = long("kafka_offset")
    override val primaryKey: PrimaryKey = PrimaryKey(version, partition)
}