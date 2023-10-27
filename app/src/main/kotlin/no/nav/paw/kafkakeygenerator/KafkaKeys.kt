package no.nav.paw.kafkakeygenerator

import no.nav.paw.kafkakeygenerator.database.IdentitetTabell
import no.nav.paw.kafkakeygenerator.database.KafkaKeysTabell
import no.nav.paw.kafkakeygenerator.vo.Identitetsnummer
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

interface KafkaKeys {

    fun hent(identitet: Identitetsnummer): Long?
    fun lagre(identitet: Identitetsnummer, nøkkel: Long): Boolean
    fun opprett(identitet: Identitetsnummer): Long?
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
    override fun hent(identitet: Identitetsnummer): Long? =
        transaction(database) {
            IdentitetTabell.select {
                IdentitetTabell.identitetsnummer eq identitet.value
            }.firstOrNull()?.get(IdentitetTabell.kafkaKey)
        }

    override fun lagre(identitet: Identitetsnummer, nøkkel: Long): Boolean =
        transaction(database) {
            IdentitetTabell.insertIgnore {
                it[identitetsnummer] = identitet.value
                it[kafkaKey] = nøkkel
            }.insertedCount == 1
        }

    override fun opprett(identitet: Identitetsnummer): Long? =
        transaction(database) {
            val nøkkel = KafkaKeysTabell.insert { }[KafkaKeysTabell.id]
            val opprettet = IdentitetTabell.insertIgnore {
                it[identitetsnummer] = identitet.value
                it[kafkaKey] = nøkkel
            }.insertedCount == 1
            if (opprettet) nøkkel else null
        }
}