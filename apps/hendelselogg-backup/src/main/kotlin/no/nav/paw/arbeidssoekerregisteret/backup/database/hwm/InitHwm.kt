package no.nav.paw.arbeidssoekerregisteret.backup.database.hwm

import no.nav.paw.arbeidssoekerregisteret.backup.context.ApplicationContext
import org.jetbrains.exposed.sql.transactions.transaction

fun initHwm(applicationContext: ApplicationContext) = with(applicationContext) {
    val partitions = applicationConfig.partitionCount
    val version = applicationConfig.consumerVersion
    transaction {
        initHwm(version, partitions)
        metrics.registerHwmGauges(applicationContext, getAllHwms(version))
    }
}
