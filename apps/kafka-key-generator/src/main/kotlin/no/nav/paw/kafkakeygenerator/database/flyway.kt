package no.nav.paw.kafkakeygenerator.database

fun flywayMigrate(dataSource: javax.sql.DataSource) {
    org.flywaydb.core.Flyway.configure()
        .dataSource(dataSource)
        .baselineOnMigrate(true)
        .load()
        .migrate()
}