package no.nav.paw.arbeidssoekerregisteret.backup.metrics

import io.micrometer.core.instrument.Tag
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.backup.context.ApplicationContext
import no.nav.paw.arbeidssoekerregisteret.backup.database.hwm.Hwm
import no.nav.paw.arbeidssoekerregisteret.backup.database.hwm.getHwm
import no.nav.paw.arbeidssoekerregisteret.backup.kafka.HwmRebalanceListener
import no.nav.paw.arbeidssokerregisteret.intern.v1.Aarsak

class Metrics(
    private val prometheusMeterRegistry: PrometheusMeterRegistry,
) {
    companion object {
        const val ACTIVE_PARTITIONS_GAUGE = "paw_arbeidssoekerregisteret_hendelselogg_backup_active_partitions"
        const val HWM_GAUGE = "paw_arbeidssoekerregisteret_hendelselogg_backup_hwm"
        const val RECORD_COUNTER = "paw_arbeidssoekerregisteret_hendelselogg_backup_records_written"
        const val KALKULERT_AVSLUTTET_AARSAK = "paw_arbeidssoekerregisteret_hendelselogg_backup_kalkulert_avsluttet_aarsak"
    }

    fun createActivePartitionsGauge(hwmRebalanceListener: HwmRebalanceListener): HwmRebalanceListener? =
        prometheusMeterRegistry.gauge(
            ACTIVE_PARTITIONS_GAUGE,
            hwmRebalanceListener
        ) { it.currentlyAssignedPartitions.size.toDouble() }


    fun registerHwmGauges(
        applicationContext: ApplicationContext,
        allHwms: List<Hwm>,
    ) {
        allHwms.forEach { hwm ->
            val tags = listOf(Tag.of("partition", hwm.partition.toString()))
            prometheusMeterRegistry.gauge(HWM_GAUGE, tags, applicationContext) {
                getHwm(applicationContext.applicationConfig.consumerVersion, hwm.partition)?.toDouble() ?: -1.0
            }
        }
    }

    val recordCounter = prometheusMeterRegistry.counter(RECORD_COUNTER, listOf(Tag.of("include", "true")))
    val duplicateRecordCounter = prometheusMeterRegistry.counter(RECORD_COUNTER, listOf(Tag.of("include", "false")))

    val kalkulertAvsluttetAarsakCounters = Aarsak.entries.associateWith { aarsak ->
        prometheusMeterRegistry.counter(
            KALKULERT_AVSLUTTET_AARSAK,
            listOf(Tag.of("kalkulert_aarsak", aarsak.name))
        )
    }
}
