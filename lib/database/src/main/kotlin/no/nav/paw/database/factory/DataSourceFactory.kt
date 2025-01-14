package no.nav.paw.database.factory

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.paw.database.config.DatabaseConfig

fun createHikariDataSource(databaseConfig: DatabaseConfig): HikariDataSource =
    HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = databaseConfig.url
            maximumPoolSize = databaseConfig.maximumPoolSize
            connectionTimeout = databaseConfig.connectionTimeout.toMillis()
            maxLifetime = databaseConfig.maxLifetime.toMillis()
        }
    )
