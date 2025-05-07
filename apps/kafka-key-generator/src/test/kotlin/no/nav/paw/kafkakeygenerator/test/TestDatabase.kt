package no.nav.paw.kafkakeygenerator.test

import no.nav.paw.database.config.DatabaseConfig
import no.nav.paw.database.factory.createHikariDataSource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import javax.sql.DataSource

fun postgresTestDatabase(): DataSource {
    val config = postgreSQLContainer().let {
        DatabaseConfig(
            host = it.host,
            port = it.firstMappedPort,
            database = it.databaseName,
            username = it.username,
            password = it.password,
            autoCommit = false
        )
    }
    return createHikariDataSource(config)
}

fun postgreSQLContainer(): PostgreSQLContainer<out PostgreSQLContainer<*>> {
    val postgres = PostgreSQLContainer(
        "postgres:14"
    ).apply {
        addEnv("POSTGRES_PASSWORD", "admin")
        addEnv("POSTGRES_USER", "admin")
        addEnv("POSTGRES_DATABASE", "pawkafkakeys")
        addExposedPorts(5432)
    }
    postgres.start()
    postgres.waitingFor(Wait.forHealthcheck())
    return postgres
}
