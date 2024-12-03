package no.nav.paw.kafkakeygenerator.plugin

import io.ktor.server.application.Application
import io.ktor.server.application.install
import no.nav.paw.kafkakeygenerator.plugin.custom.FlywayPlugin
import javax.sql.DataSource

fun Application.configureDatabase(dataSource: DataSource) {
    install(FlywayPlugin) {
        this.dataSource = dataSource
    }
}
