package no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.database

import org.jetbrains.exposed.sql.Table

object BekreftelseHwmTable: Table("bekreftelse_hwm") {
    val version = integer("version")
    val partition = integer("kafka_partition")
    val offset = long("kafka_offset")
    val topic = varchar("kafka_topic", 256)
    override val primaryKey: PrimaryKey = PrimaryKey(version, partition, topic)
}