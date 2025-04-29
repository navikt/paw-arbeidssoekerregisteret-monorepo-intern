package no.nav.paw.bekreftelse.api.test

import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.database.config.DATABASE_CONFIG
import no.nav.paw.database.config.DatabaseConfig
import no.nav.paw.database.factory.createHikariDataSource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import javax.sql.DataSource

fun createTestDataSource(
    databaseConfig: DatabaseConfig = loadNaisOrLocalConfiguration(DATABASE_CONFIG),
    postgresContainer: PostgreSQLContainer<*> = postgresContainer(),
): DataSource {
    val updatedDatabaseConfig = postgresContainer.let {
        databaseConfig.copy(
            host = it.host,
            port = it.firstMappedPort,
            username = it.username,
            password = it.password,
            database = it.databaseName
        )
    }
    return createHikariDataSource(updatedDatabaseConfig)
}

private fun postgresContainer(): PostgreSQLContainer<out PostgreSQLContainer<*>> {
    val postgres = PostgreSQLContainer("postgres:17").apply {
        addEnv("POSTGRES_PASSWORD", "bekreftelse_api")
        addEnv("POSTGRES_USER", "Paw1234")
        addEnv("POSTGRES_DB", "bekreftelser")
        addExposedPorts(5432)
    }
    postgres.start()
    postgres.waitingFor(Wait.forHealthcheck())
    return postgres
}
