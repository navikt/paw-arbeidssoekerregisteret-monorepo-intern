package no.nav.paw.kafkakeygenerator.database

import no.nav.paw.kafkakeygenerator.vo.IdentitetStatus
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.timestamp

object KafkaKeysAuditTable : LongIdTable("kafka_keys_audit") {
    val identitetsnummer = varchar("identitetsnummer", 255).references(IdentitetTabell.identitetsnummer)
    val status = enumerationByName<IdentitetStatus>("status", 50)
    val detaljer = varchar("detaljer", 255)
    val tidspunkt = timestamp("tidspunkt")
}
