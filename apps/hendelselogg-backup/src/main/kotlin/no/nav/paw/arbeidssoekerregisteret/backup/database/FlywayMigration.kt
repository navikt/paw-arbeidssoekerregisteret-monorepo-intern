package no.nav.paw.arbeidssoekerregisteret.backup.database

import org.flywaydb.core.Flyway

fun migrateDatabase(dataSource: javax.sql.DataSource) {
    Flyway.configure()
        .dataSource(dataSource)
        .locations("classpath:db/migration")
        .baselineOnMigrate(true)
        .load()
        .migrate()
}