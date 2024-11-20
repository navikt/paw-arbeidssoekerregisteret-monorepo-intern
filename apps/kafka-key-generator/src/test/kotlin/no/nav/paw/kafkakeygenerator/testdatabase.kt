package no.nav.paw.kafkakeygenerator

import no.nav.paw.kafkakeygenerator.config.DatabaseConfig
import no.nav.paw.kafkakeygenerator.database.createDataSource
import no.nav.paw.kafkakeygenerator.database.flywayMigrate
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import javax.sql.DataSource

fun initTestDatabase(): DataSource {
    val config = postgreSQLContainer().let {
        DatabaseConfig(
            jdbcUrl = "jdbc:postgresql://${it.host}:${it.firstMappedPort}/${it.databaseName}?user=${it.username}&password=${it.password}",
            driverClassName = "org.postgresql.Driver",
            autoCommit = false
        )
    }
    val dataSource = createDataSource(config)
    flywayMigrate(dataSource)
    return dataSource
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