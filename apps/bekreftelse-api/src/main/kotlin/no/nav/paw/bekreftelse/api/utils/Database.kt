package no.nav.paw.bekreftelse.api.utils

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.paw.bekreftelse.api.config.DatabaseConfig
import javax.sql.DataSource

fun createDataSource(config: DatabaseConfig): DataSource {
    return HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = config.jdbcUrl
            driverClassName = config.driverClassName
            isAutoCommit = config.autoCommit
            maximumPoolSize = config.maxPoolSize
            connectionTimeout = config.connectionTimeout.toMillis()
            idleTimeout = config.idleTimeout.toMillis()
            maxLifetime = config.maxLifetime.toMillis()
        }
    )
}