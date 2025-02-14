package no.nav.paw.arbeidssoeker.synk.utils

import org.flywaydb.core.Flyway
import javax.sql.DataSource

fun DataSource.flywayMigrate(): DataSource {
    Flyway.configure()
        .dataSource(this)
        .baselineOnMigrate(true)
        .load()
        .migrate()
    return this
}
