package no.nav.paw.kafkakeygenerator.repository

import no.nav.paw.kafkakeygenerator.database.KafkaKeysAuditTable
import no.nav.paw.kafkakeygenerator.vo.Audit
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction

class KafkaKeysAuditRepository(
    private val database: Database
) {

    fun insert(audit: Audit): Int = transaction(database) {
        KafkaKeysAuditTable.insert {
            it[identitetsnummer] = audit.identitetsnummer.value
            it[status] = audit.identitetStatus
            it[detaljer] = audit.detaljer
            it[tidspunkt] = audit.tidspunkt
        }.insertedCount
    }
}