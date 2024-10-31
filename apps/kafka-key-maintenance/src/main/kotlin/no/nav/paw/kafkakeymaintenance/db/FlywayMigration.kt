package no.nav.paw.kafkakeymaintenance.db

import org.flywaydb.core.Flyway

fun migrateDatabase(dataSource: javax.sql.DataSource) {
    Flyway.configure()
        .dataSource(dataSource)
        .locations("classpath:db/migration")
        .baselineOnMigrate(true)
        .load()
        .migrate()
}