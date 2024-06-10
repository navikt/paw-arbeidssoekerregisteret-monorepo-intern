package no.nav.paw.arbeidssoekerregisteret.backup.database

import org.jetbrains.exposed.sql.Table

object HendelseTable: Table("hendelser") {
    val version = integer("version")
    val partition = integer("kafka_partition")
    val offset = long("kafka_offset")
    val recordKey = long("record_key")
    val arbeidssoekerId = long("arbeidssoeker_id")
    val traceparent = varchar("traceparent", 58).nullable()
    val data = jsonb("data")
    override val primaryKey: PrimaryKey = PrimaryKey(version, partition, offset)
}