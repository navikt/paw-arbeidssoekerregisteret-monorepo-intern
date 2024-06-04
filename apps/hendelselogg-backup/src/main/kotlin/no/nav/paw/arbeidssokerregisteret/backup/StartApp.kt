package no.nav.paw.arbeidssokerregisteret.backup

import no.nav.paw.arbeidssokerregisteret.backup.database.DatabaseConfig
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.config.kafka.KAFKA_CONFIG
import no.nav.paw.config.kafka.KafkaConfig

fun main() {
    val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_CONFIG)
    val databaseConfig = loadNaisOrLocalConfiguration<DatabaseConfig>("database_configuration.toml")
}