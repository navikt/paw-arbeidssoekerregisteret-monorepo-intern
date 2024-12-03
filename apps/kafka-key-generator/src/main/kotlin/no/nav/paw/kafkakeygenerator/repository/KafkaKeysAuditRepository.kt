package no.nav.paw.kafkakeygenerator.repository

import no.nav.paw.kafkakeygenerator.database.KafkaKeysAuditTable
import no.nav.paw.kafkakeygenerator.vo.ArbeidssoekerId
import no.nav.paw.kafkakeygenerator.vo.Audit
import no.nav.paw.kafkakeygenerator.vo.IdentitetStatus
import no.nav.paw.kafkakeygenerator.vo.Identitetsnummer
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class KafkaKeysAuditRepository(
    private val database: Database
) {

    fun findByIdentitetsnummer(identitetsnummer: Identitetsnummer): List<Audit> = transaction(database) {
        KafkaKeysAuditTable.selectAll()
            .where(KafkaKeysAuditTable.identitetsnummer eq identitetsnummer.value)
            .map {
                Audit(
                    identitetsnummer = Identitetsnummer(it[KafkaKeysAuditTable.identitetsnummer]),
                    tidligereArbeidssoekerId = ArbeidssoekerId(it[KafkaKeysAuditTable.tidligereKafkaKey]),
                    identitetStatus = it[KafkaKeysAuditTable.status],
                    detaljer = it[KafkaKeysAuditTable.detaljer],
                    tidspunkt = it[KafkaKeysAuditTable.tidspunkt]
                )
            }
    }

    fun findByStatus(status: IdentitetStatus): List<Audit> = transaction(database) {
        KafkaKeysAuditTable.selectAll()
            .where(KafkaKeysAuditTable.status eq status)
            .map {
                Audit(
                    identitetsnummer = Identitetsnummer(it[KafkaKeysAuditTable.identitetsnummer]),
                    tidligereArbeidssoekerId = ArbeidssoekerId(it[KafkaKeysAuditTable.tidligereKafkaKey]),
                    identitetStatus = it[KafkaKeysAuditTable.status],
                    detaljer = it[KafkaKeysAuditTable.detaljer],
                    tidspunkt = it[KafkaKeysAuditTable.tidspunkt]
                )
            }
    }

    fun insert(audit: Audit): Int = transaction(database) {
        KafkaKeysAuditTable.insert {
            it[identitetsnummer] = audit.identitetsnummer.value
            it[tidligereKafkaKey] = audit.tidligereArbeidssoekerId.value
            it[status] = audit.identitetStatus
            it[detaljer] = audit.detaljer
            it[tidspunkt] = audit.tidspunkt
        }.insertedCount
    }
}