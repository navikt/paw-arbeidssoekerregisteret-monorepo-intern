package no.nav.paw.arbeidssoeker.synk.test

import no.nav.paw.database.config.DatabaseConfig
import no.nav.paw.database.factory.createHikariDataSource
import org.flywaydb.core.Flyway
import org.testcontainers.containers.PostgreSQLContainer
import javax.sql.DataSource

fun createTestDataSource(): DataSource {
    val postgres = postgresContainer()
    val databaseConfig = DatabaseConfig(
        host = postgres.host,
        port = postgres.firstMappedPort,
        username = postgres.username,
        password = postgres.password,
        database = postgres.databaseName
    )
    return createHikariDataSource(databaseConfig)
}

private fun postgresContainer(): PostgreSQLContainer<out PostgreSQLContainer<*>> {
    val postgres = PostgreSQLContainer("postgres:17").apply {
        addEnv("POSTGRES_PASSWORD", "paw_arbeidssoekere_synk_jobb")
        addEnv("POSTGRES_USER", "Paw1234")
        addEnv("POSTGRES_DB", "paw_arbeidssoekere_synk_jobb")
        addExposedPorts(5432)
    }
    postgres.start()
    return postgres
}

fun DataSource.flywayMigrate(): DataSource {
    Flyway.configure()
        .dataSource(this)
        .baselineOnMigrate(true)
        .load()
        .migrate()
    return this
}
