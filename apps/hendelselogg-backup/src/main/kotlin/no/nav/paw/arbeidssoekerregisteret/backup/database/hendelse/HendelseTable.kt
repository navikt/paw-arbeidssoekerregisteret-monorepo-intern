package no.nav.paw.arbeidssoekerregisteret.backup.database.hendelse

import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseSerde
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.json.jsonb

object HendelseTable : Table("hendelser") {
    val version = integer("version")
    val partition = integer("kafka_partition")
    val offset = long("kafka_offset")
    val recordKey = long("record_key")
    val arbeidssoekerId = long("arbeidssoeker_id")
    val traceparent = varchar("traceparent", 58).nullable()
    val data = jsonb("data", JsonbSerde::serialize, JsonbSerde::deserialize)
    override val primaryKey: PrimaryKey = PrimaryKey(version, partition, offset)
}

object JsonbSerde {
    private val serde = HendelseSerde()
    fun serialize(data: Hendelse): String = serde.serializer().serializeToString(data)
    fun deserialize(data: String) = serde.deserializer().deserializeFromString(data)
}