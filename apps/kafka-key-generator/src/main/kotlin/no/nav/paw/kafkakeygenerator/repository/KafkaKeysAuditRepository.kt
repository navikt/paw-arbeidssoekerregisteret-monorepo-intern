package no.nav.paw.kafkakeygenerator.repository

import no.nav.paw.kafkakeygenerator.database.KafkaKeysAuditTable
import no.nav.paw.kafkakeygenerator.model.ArbeidssoekerId
import no.nav.paw.kafkakeygenerator.model.Audit
import no.nav.paw.kafkakeygenerator.model.AuditIdentitetStatus
import no.nav.paw.kafkakeygenerator.model.Identitetsnummer
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class KafkaKeysAuditRepository {

    fun findByIdentitetsnummer(identitetsnummer: Identitetsnummer): List<Audit> = transaction {
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

    fun findByStatus(status: AuditIdentitetStatus): List<Audit> = transaction {
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

    fun insert(audit: Audit): Int = transaction {
        KafkaKeysAuditTable.insert {
            it[identitetsnummer] = audit.identitetsnummer.value
            it[tidligereKafkaKey] = audit.tidligereArbeidssoekerId.value
            it[status] = audit.identitetStatus
            it[detaljer] = audit.detaljer
            it[tidspunkt] = audit.tidspunkt
        }.insertedCount
    }
}