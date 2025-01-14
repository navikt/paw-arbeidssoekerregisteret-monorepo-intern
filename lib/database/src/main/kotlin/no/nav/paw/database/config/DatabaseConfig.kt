package no.nav.paw.database.config

import java.time.Duration

const val DATABASE_CONFIG = "database_config.toml"

data class DatabaseConfig(
    val host: String,
    val port: Int,
    val username: String,
    val password: String,
    val name: String,
    val maximumPoolSize: Int = 3,
    val connectionTimeout: Duration = Duration.ofSeconds(30),
    val maxLifetime: Duration = Duration.ofMinutes(30)
) {
    val url get() = "jdbc:postgresql://$host:$port/$name?user=$username&password=$password"
}
