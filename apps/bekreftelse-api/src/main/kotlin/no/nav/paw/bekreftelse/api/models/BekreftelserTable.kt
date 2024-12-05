package no.nav.paw.bekreftelse.api.models

import no.nav.paw.bekreftelse.api.utils.JsonbSerde
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.json.jsonb

object BekreftelserTable : Table("bekreftelser") {
    val version = integer("version")
    val partition = integer("kafka_partition")
    val offset = long("kafka_offset")
    val recordKey = long("record_key")
    val arbeidssoekerId = long("arbeidssoeker_id")
    val periodeId = uuid("periode_id")
    val bekreftelseId = uuid("bekreftelse_id")
    val data = jsonb("data", JsonbSerde::serialize, JsonbSerde::deserialize)
    override val primaryKey: PrimaryKey = PrimaryKey(version, partition, offset)
}