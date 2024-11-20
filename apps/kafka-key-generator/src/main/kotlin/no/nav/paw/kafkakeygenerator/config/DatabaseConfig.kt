package no.nav.paw.kafkakeygenerator.config

const val DATABASE_CONFIG = "database_config.toml"

data class DatabaseConfig(
    val host: String,
    val port: Int,
    val database: String,
    val username: String,
    val password: String,
    val driverClassName: String,
    val autoCommit: Boolean
) {
    val jdbcUrl = "jdbc:postgresql://$host:$port/$database?user=$username&password=$password"
}
