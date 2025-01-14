package no.nav.paw.database.config

import java.time.Duration

const val DATABASE_CONFIG = "database_config.toml"

data class DatabaseConfig(
    val host: String,
    val port: Int,
    val username: String,
    val password: String,
    val database: String,
    val autoCommit: Boolean = true,
    val maximumPoolSize: Int = 3,
    val connectionTimeout: Duration = Duration.ofSeconds(30),
    val idleTimeout: Duration = Duration.ofMinutes(10),
    val maxLifetime: Duration = Duration.ofMinutes(30)
) {
    val url get() = "jdbc:postgresql://$host:$port/$database?user=$username&password=$password"
}
