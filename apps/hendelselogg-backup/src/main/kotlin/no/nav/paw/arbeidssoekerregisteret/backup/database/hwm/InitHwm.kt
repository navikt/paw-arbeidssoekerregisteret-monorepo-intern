package no.nav.paw.arbeidssoekerregisteret.backup.database.hwm

import io.micrometer.core.instrument.Tag
import no.nav.paw.arbeidssoekerregisteret.backup.context.ApplicationContext
import no.nav.paw.arbeidssoekerregisteret.backup.metrics.Metrics
import org.jetbrains.exposed.sql.transactions.transaction

fun initHwm(context: ApplicationContext) {
    val partitions = context.applicationConfig.partitionCount
    val version = context.applicationConfig.version
    val allHwms = transaction {
            initHwm(version, partitions)
        getAllHwms(version)
    }
    allHwms.forEach { hwm ->
        context.prometheusMeterRegistry.gauge(Metrics.HWM_GAUGE, listOf(Tag.of("partition", hwm.partition.toString())), context) { _ ->
            transaction {
                getHwm(version, hwm.partition)?.toDouble() ?: -1.0
            }
        }
    }
}
