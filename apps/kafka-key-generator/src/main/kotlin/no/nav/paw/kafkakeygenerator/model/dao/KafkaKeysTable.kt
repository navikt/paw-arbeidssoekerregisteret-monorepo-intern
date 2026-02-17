package no.nav.paw.kafkakeygenerator.model.dao

import no.nav.paw.felles.model.ArbeidssoekerId
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object KafkaKeysTable : Table("KafkaKeys") {
    val id = long("id").autoIncrement()
    override val primaryKey = PrimaryKey(id, name = "PK_KafkaKeys")

    fun insert(): ArbeidssoekerId = transaction {
        val id = KafkaKeysTable.insert { }[KafkaKeysTable.id]
        ArbeidssoekerId(id)
    }
}