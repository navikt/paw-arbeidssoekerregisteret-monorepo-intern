package no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup

import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.database.DatabaseConfig
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.database.dataSource
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.database.migrateDatabase
import org.jetbrains.exposed.sql.Database
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import javax.sql.DataSource

fun initDbContainer(): Pair<Database, DataSource> {
    val databaseName = "bekreftelseBackupTest"
    val postgres = PostgreSQLContainer("postgres:17")
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