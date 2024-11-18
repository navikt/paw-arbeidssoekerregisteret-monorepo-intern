package no.nav.paw.kafkakeymaintenance

import no.nav.paw.kafkakeymaintenance.db.DatabaseConfig
import no.nav.paw.kafkakeymaintenance.db.dataSource
import no.nav.paw.kafkakeymaintenance.db.migrateDatabase
import org.jetbrains.exposed.sql.Database
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import javax.sql.DataSource

fun initDbContainer(databaseName: String): Pair<Database, DataSource> {
    val postgres = PostgreSQLContainer("postgres:16")
        .withDatabaseName(databaseName)
        .withUsername("postgres")
        .withPassword("postgres")

    postgres.start()
    postgres.waitingFor(Wait.forHealthcheck())
    val dbConfig = postgres.databaseConfig()
    val dataSource = dbConfig.dataSource()
    migrateDatabase(dataSource)
    return Database.connect(dataSource) to dataSource
}

fun PostgreSQLContainer<*>.databaseConfig(): DatabaseConfig {
    return DatabaseConfig(
        host = host,
        port = firstMappedPort,
        username = username,
        password = password,
        name = databaseName,
        jdbc = null
    )
}