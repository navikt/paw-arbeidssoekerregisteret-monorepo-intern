package no.nav.paw.arbeidssoekerregisteret.backup.database

import org.flywaydb.core.Flyway
import javax.sql.DataSource

fun migrateDatabase(dataSource: DataSource) {
    Flyway.configure()
        .dataSource(dataSource)
        .locations("classpath:db/migration")
        .baselineOnMigrate(true)
        .load()
        .migrate()
}