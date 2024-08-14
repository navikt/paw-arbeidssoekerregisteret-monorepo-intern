package no.nav.paw.kafkakeygenerator

import no.nav.paw.kafkakeygenerator.config.DatabaseKonfigurasjon
import no.nav.paw.kafkakeygenerator.config.dataSource
import no.nav.paw.kafkakeygenerator.database.flywayMigrate
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import javax.sql.DataSource

fun initTestDatabase(): DataSource {
    val postgres = postgreSQLContainer()
    val dataSource = DatabaseKonfigurasjon(
        host = postgres.host,
        port = postgres.firstMappedPort,
        brukernavn = postgres.username,
        passord = postgres.password,
        databasenavn = postgres.databaseName
    ).dataSource()
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