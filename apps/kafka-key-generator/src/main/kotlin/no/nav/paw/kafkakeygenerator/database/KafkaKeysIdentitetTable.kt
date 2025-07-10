package no.nav.paw.kafkakeygenerator.database

import org.jetbrains.exposed.sql.Table

object KafkaKeysIdentitetTable: Table("Identitet") {
    val identitetsnummer = varchar("identitetsnummer", 255)
    val kafkaKey = long("kafka_key").references(KafkaKeysTable.id)
    override val primaryKey = PrimaryKey(identitetsnummer, name = "Identitet_pkey")
}