package no.nav.paw.arbeidssoekerregisteret.backup.database.hwm

import no.nav.paw.arbeidssoekerregisteret.backup.context.ApplicationContext
import org.jetbrains.exposed.sql.transactions.transaction

fun initHwm(applicationContext: ApplicationContext) = with(applicationContext) {
    val partitions = applicationConfig.partitionCount
    val version = applicationConfig.consumerVersion
    val allHwms = transaction {
        initHwm(version, partitions)
        getAllHwms(version)
    }
    metrics.registerHwmGauges(applicationContext, allHwms)
}
