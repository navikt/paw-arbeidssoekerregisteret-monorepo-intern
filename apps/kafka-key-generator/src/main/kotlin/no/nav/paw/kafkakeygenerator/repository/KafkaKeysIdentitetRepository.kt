package no.nav.paw.kafkakeygenerator.repository

import no.nav.paw.kafkakeygenerator.database.KafkaKeysIdentitetTable
import no.nav.paw.kafkakeygenerator.model.KafkaKeyRow
import no.nav.paw.kafkakeygenerator.model.asKafkaKeyRow
import no.nav.paw.kafkakeygenerator.model.ArbeidssoekerId
import no.nav.paw.kafkakeygenerator.model.Identitetsnummer
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class KafkaKeysIdentitetRepository {
    fun find(identitetsnummer: Identitetsnummer): KafkaKeyRow? = transaction {
        KafkaKeysIdentitetTable.selectAll()
            .where { KafkaKeysIdentitetTable.identitetsnummer eq identitetsnummer.value }
            .map { it.asKafkaKeyRow() }
            .singleOrNull()
    }

    fun findByIdentiteter(identiteter: Collection<String>): List<KafkaKeyRow> = transaction {
        KafkaKeysIdentitetTable.selectAll()
            .where { KafkaKeysIdentitetTable.identitetsnummer inList identiteter }
            .map { it.asKafkaKeyRow() }
    }

    fun insert(
        ident: Identitetsnummer,
        arbeidssoekerId: ArbeidssoekerId
    ): Int = transaction {
        KafkaKeysIdentitetTable.insert {
            it[identitetsnummer] = ident.value
            it[kafkaKey] = arbeidssoekerId.value
        }.insertedCount
    }

    fun update(
        identitetsnummer: Identitetsnummer,
        tilArbeidssoekerId: ArbeidssoekerId
    ): Int = transaction {
        KafkaKeysIdentitetTable.update(where = {
            (KafkaKeysIdentitetTable.identitetsnummer eq identitetsnummer.value)
        }) {
            it[kafkaKey] = tilArbeidssoekerId.value
        }
    }
}