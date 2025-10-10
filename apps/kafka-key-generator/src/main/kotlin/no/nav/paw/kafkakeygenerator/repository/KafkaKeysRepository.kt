package no.nav.paw.kafkakeygenerator.repository

import no.nav.paw.kafkakeygenerator.database.KafkaKeysTable
import no.nav.paw.kafkakeygenerator.model.ArbeidssoekerId
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction

class KafkaKeysRepository {

    fun opprett(): ArbeidssoekerId = transaction {
        val id = KafkaKeysTable.insert { }[KafkaKeysTable.id]
        ArbeidssoekerId(id)
    }
}