package no.nav.paw.kafkakeygenerator.database

import org.jetbrains.exposed.sql.Table

object IdentitetTabell: Table("Identitet") {
    val identitetsnummer = varchar("identitetsnummer", 255)
    val kafkaKey = long("kafka_key").references(KafkaKeysTabell.id)
    override val primaryKey = PrimaryKey(identitetsnummer, name = "Identitet_pkey")
}