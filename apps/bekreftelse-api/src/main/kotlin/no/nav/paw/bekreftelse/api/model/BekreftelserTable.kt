package no.nav.paw.bekreftelse.api.model

import no.nav.paw.bekreftelse.api.utils.JsonSerde
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.json.jsonb

object BekreftelserTable : Table("bekreftelser") {
    val version = integer("version")
    val partition = integer("kafka_partition")
    val offset = long("kafka_offset")
    val recordKey = long("record_key")
    val arbeidssoekerId = long("arbeidssoeker_id")
    val periodeId = javaUUID("periode_id")
    val bekreftelseId = javaUUID("bekreftelse_id")
    val data = jsonb("data", JsonSerde::serialize, JsonSerde::deserialize)
    override val primaryKey: PrimaryKey = PrimaryKey(version, partition, offset)
}