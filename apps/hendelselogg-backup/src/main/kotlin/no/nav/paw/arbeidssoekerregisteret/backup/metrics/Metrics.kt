package no.nav.paw.arbeidssoekerregisteret.backup.metrics

class Metrics() {
    companion object {
        const val ACTIVE_PARTITIONS_GAUGE = "paw_arbeidssoekerregisteret_hendelselogg_backup_active_partitions"
        const val RECORD_COUNTER = "paw_arbeidssoekerregisteret_hendelselogg_backup_records_written"
        const val KALKULERT_AVSLUTTET_AARSAK = "paw_arbeidssoekerregisteret_hendelselogg_backup_kalkulert_avsluttet_aarsak"
        const val HWM_GAUGE = "paw_arbeidssoekerregisteret_hendelselogg_backup_hwm"
    }
}