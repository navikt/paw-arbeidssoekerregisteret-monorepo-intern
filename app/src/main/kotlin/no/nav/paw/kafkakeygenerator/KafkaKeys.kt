package no.nav.paw.kafkakeygenerator

import no.nav.paw.kafkakeygenerator.database.IdentitetTabell
import no.nav.paw.kafkakeygenerator.database.KafkaKeysTabell
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction

interface KafkaKeys {

    fun hent(identitet: String): Long?
    fun lagre(identitet: String, nøkkel: Long): Boolean
    fun opprett(identitet: String): Long?
    fun hent(identiteter: List<String>): Map<String, Long>
}

class ExposedKafkaKeys(private val database: Database) : KafkaKeys {

    override fun hent(identiteter: List<String>): Map<String, Long> =
        transaction(database) {
            IdentitetTabell.select {
                IdentitetTabell.identitetsnummer inList identiteter
            }.associate {
                it[IdentitetTabell.identitetsnummer] to it[IdentitetTabell.kafkaKey]
            }
        }
    override fun hent(identitet: String): Long? =
        transaction(database) {
            IdentitetTabell.select {
                IdentitetTabell.identitetsnummer eq identitet
            }.firstOrNull()?.get(IdentitetTabell.kafkaKey)
        }

    override fun lagre(identitet: String, nøkkel: Long): Boolean =
        transaction(database) {
            IdentitetTabell.insertIgnore {
                it[identitetsnummer] = identitet
                it[kafkaKey] = nøkkel
            }.insertedCount == 1
        }

    override fun opprett(identitet: String): Long? =
        transaction(database) {
            val nøkkel = KafkaKeysTabell.insert { }[KafkaKeysTabell.id]
            val opprettet = IdentitetTabell.insertIgnore {
                it[identitetsnummer] = identitet
                it[kafkaKey] = nøkkel
            }.insertedCount == 1
            if (opprettet) nøkkel else null
        }
}