package no.nav.paw.database.plugin

import io.ktor.server.application.Application
import io.ktor.server.application.install
import javax.sql.DataSource

fun Application.installDatabasePlugin(dataSource: DataSource) {
    install(DataSourcePlugin) {
        this.dataSource = dataSource
    }
    install(FlywayPlugin) {
        this.dataSource = dataSource
    }
}
