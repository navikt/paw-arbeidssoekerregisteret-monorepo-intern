package no.nav.paw.arbeidssoekerregisteret.backup

import no.nav.paw.arbeidssoekerregisteret.backup.database.dataSource
import no.nav.paw.arbeidssoekerregisteret.backup.database.migrateDatabase
import org.jetbrains.exposed.sql.Database
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import javax.sql.DataSource

fun initDbContainer(): Pair<Database, DataSource> {
    val databaseName = "backupTest"
    val postgres = PostgreSQLContainer("postgres:15")
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