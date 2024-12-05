package no.nav.paw.bekreftelse.api.plugin

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationPlugin
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.hooks.MonitoringEvent
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.util.KtorDsl
import no.nav.paw.bekreftelse.api.models.BekreftelseRow
import no.nav.paw.bekreftelse.api.plugins.custom.FlywayMigrationCompleted
import no.nav.paw.bekreftelse.api.repository.BekreftelseRepository
import no.nav.paw.bekreftelse.api.test.TestData
import org.jetbrains.exposed.sql.transactions.transaction

@KtorDsl
class TestDataPluginConfig {
    var testData: TestData? = null

    companion object {
        const val PLUGIN_NAME = "TestDataPlugin"
    }
}

val TestDataPlugin: ApplicationPlugin<TestDataPluginConfig> =
    createApplicationPlugin(TestDataPluginConfig.PLUGIN_NAME, ::TestDataPluginConfig) {
        application.log.info("Oppretter {}", TestDataPluginConfig.PLUGIN_NAME)
        val testData = requireNotNull(pluginConfig.testData) { "TestData er null" }
        val bekreftelseRepository = BekreftelseRepository()

        on(MonitoringEvent(FlywayMigrationCompleted)) { application ->
            application.log.info("Oppretter testdata")
            bekreftelseRepository.insertBekreftelseRows(testData.bereftelseRows)
        }
    }

private fun BekreftelseRepository.insertBekreftelseRows(bereftelseRows: List<BekreftelseRow>) {
    transaction {
        bereftelseRows.forEach { insert(it) }
    }
}

fun Application.configTestDataPlugin(testData: TestData) {
    install(TestDataPlugin) {
        this.testData = testData
    }
}