package no.nav.paw.health.probe

import no.nav.paw.health.model.LivenessCheck
import org.slf4j.LoggerFactory
import javax.sql.DataSource

class DataSourceHealthProbe(private val dataSource: DataSource) : LivenessCheck {
    private val logger = LoggerFactory.getLogger("no.nav.paw.logger.health.database")

    override fun isAlive(): Boolean = runCatching {
        dataSource.connection.use { connection ->
            connection.prepareStatement("SELECT 1").execute()
        }
    }.onFailure { error ->
        logger.warn("Databasen er ikke klar", error)
    }.getOrDefault(false)
}
