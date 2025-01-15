package no.nav.paw.database.config

import java.time.Duration

const val DATABASE_CONFIG = "database_config.toml"

data class DatabaseConfig(
    val jdbcUrl: String? = null,
    val url: String? = null,
    val host: String? = null,
    val port: Int? = null,
    val username: String? = null,
    val password: String? = null,
    val database: String? = null,
    val autoCommit: Boolean = true,
    val maximumPoolSize: Int = 3,
    val connectionTimeout: Duration = Duration.ofSeconds(30),
    val idleTimeout: Duration = Duration.ofMinutes(10),
    val maxLifetime: Duration = Duration.ofMinutes(30)
) {
    fun buildJdbcUrl(): String {
        return if (jdbcUrl != null) {
            require(jdbcUrl.isNotBlank()) { "JDBC URL er tom" }
            jdbcUrl
        } else if (url != null) {
            require(url.isNotBlank()) { "URL er tom" }
            "jdbc:$url"
        } else {
            require(!host.isNullOrBlank()) { "Host er ikke satt" }
            requireNotNull(port) { "Port er ikke satt" }
            require(!username.isNullOrBlank()) { "Username er ikke satt" }
            require(!password.isNullOrBlank()) { "Password er ikke satt" }
            require(!database.isNullOrBlank()) { "Database er ikke satt" }
            "jdbc:postgresql://$host:$port/$database?user=$username&password=$password"
        }
    }
}
