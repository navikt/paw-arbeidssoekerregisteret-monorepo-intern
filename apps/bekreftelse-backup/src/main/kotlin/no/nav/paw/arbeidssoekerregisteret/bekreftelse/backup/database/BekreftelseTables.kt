package no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.database

import org.jetbrains.exposed.sql.Table

object BekreftelseHendelserTable: Table("bekreftelse_hendelser") {
    val version = integer("version")
    val partition = integer("kafka_partition")
    val offset = long("kafka_offset")
    val recordKey = long("record_key")
    val arbeidssoekerId = long("arbeidssoeker_id")
    val traceparent = varchar("traceparent", 58).nullable()
    val data = jsonb("data")
    override val primaryKey: PrimaryKey = PrimaryKey(version, partition, offset)
}

object BekreftelserTable: Table("bekreftelser") {
    val version = integer("version")
    val partition = integer("kafka_partition")
    val offset = long("kafka_offset")
    val recordKey = long("record_key")
    val traceparent = varchar("traceparent", 58).nullable()
    val data = binary("data")
    override val primaryKey: PrimaryKey = PrimaryKey(version, partition, offset)
}

object PaaVegneAvTable: Table("bekreftelse_paa_vegne_av") {
    val version = integer("version")
    val partition = integer("kafka_partition")
    val offset = long("kafka_offset")
    val recordKey = long("record_key")
    val traceparent = varchar("traceparent", 58).nullable()
    val data = binary("data")
    override val primaryKey: PrimaryKey = PrimaryKey(version, partition, offset)
}