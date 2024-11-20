package no.nav.paw.kafkakeygenerator.config

const val DATABASE_CONFIG = "database_config.toml"

data class DatabaseConfig(
    val jdbcUrl: String,
    val driverClassName: String,
    val autoCommit: Boolean
)
