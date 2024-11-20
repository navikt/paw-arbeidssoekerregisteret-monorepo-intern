package no.nav.paw.kafkakeygenerator.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.paw.kafkakeygenerator.config.DatabaseConfig
import javax.sql.DataSource

fun createDataSource(config: DatabaseConfig): DataSource =
    HikariDataSource(HikariConfig().apply {
        jdbcUrl = config.jdbcUrl
        driverClassName = config.driverClassName
        isAutoCommit = config.autoCommit
    })