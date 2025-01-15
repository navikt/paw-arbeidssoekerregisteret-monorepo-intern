package no.nav.paw.bekreftelse.api.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import no.nav.paw.bekreftelse.api.context.ApplicationContext
import no.nav.paw.database.plugin.DataSourcePlugin
import no.nav.paw.database.plugin.FlywayPlugin

fun Application.configureDatabase(applicationContext: ApplicationContext) {
    install(DataSourcePlugin) {
        dataSource = applicationContext.dataSource
    }
    install(FlywayPlugin) {
        dataSource = applicationContext.dataSource
    }
}
