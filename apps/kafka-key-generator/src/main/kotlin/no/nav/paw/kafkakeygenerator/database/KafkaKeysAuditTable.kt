package no.nav.paw.kafkakeygenerator.database

import no.nav.paw.kafkakeygenerator.model.AuditIdentitetStatus
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.timestamp

object KafkaKeysAuditTable : LongIdTable("kafka_keys_audit") {
    val identitetsnummer = varchar("identitetsnummer", 255).references(KafkaKeysIdentitetTable.identitetsnummer)
    val tidligereKafkaKey = long("tidligere_kafka_key")
    val status = enumerationByName<AuditIdentitetStatus>("status", 50)
    val detaljer = varchar("detaljer", 255)
    val tidspunkt = timestamp("tidspunkt")
}
