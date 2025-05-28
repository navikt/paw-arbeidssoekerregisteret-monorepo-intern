package no.nav.paw.arbeidssoekerregisteret.backup.database.hwm

import no.nav.paw.arbeidssoekerregisteret.backup.context.ApplicationContext

fun initHwm(applicationContext: ApplicationContext) = with(applicationContext) {
    val partitions = applicationConfig.partitionCount
    val version = applicationConfig.consumerVersion
    initHwm(version, partitions)
    metrics.registerHwmGauges(applicationContext, getAllHwms(version))
}
