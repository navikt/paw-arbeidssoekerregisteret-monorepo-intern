package no.nav.paw.arbeidssoekerregisteret.backup.kafka

import io.micrometer.core.instrument.Tag
import no.nav.paw.arbeidssoekerregisteret.backup.database.updateHwm
import no.nav.paw.arbeidssoekerregisteret.backup.metrics.Metrics
import no.nav.paw.arbeidssoekerregisteret.backup.context.ApplicationContext
import no.nav.paw.arbeidssoekerregisteret.backup.database.RecordPostgresRepository
import no.nav.paw.arbeidssokerregisteret.intern.v1.Aarsak
import no.nav.paw.arbeidssokerregisteret.intern.v1.Avsluttet
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseSerializer
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.sequences.forEach


fun processRecords(records: ConsumerRecords<Long, Hendelse>, context: ApplicationContext) {
    val counterInclude = context.prometheusMeterRegistry.counter(Metrics.RECORD_COUNTER, listOf(Tag.of("include", "true")))
    val counterExclude = context.prometheusMeterRegistry.counter(Metrics.RECORD_COUNTER, listOf(Tag.of("include", "false")))

    val kalkulertAarsakCounters = Aarsak.entries.associateWith { aarsak ->
        context.prometheusMeterRegistry.counter(
            Metrics.KALKULERT_AVSLUTTET_AARSAK,
            listOf(Tag.of("kalkulert_aarsak", aarsak.name))
        )
    }

    transaction {
        records.asSequence().forEach { record ->
            if (updateHwm(context.applicationConfig.version, record.partition(), record.offset())) {
                RecordPostgresRepository.writeRecord(context.applicationConfig.version, HendelseSerializer(), record)
                counterInclude.increment()

                val hendelse = record.value()
                if (hendelse is Avsluttet) {
                    kalkulertAarsakCounters[hendelse.kalkulertAarsak]?.increment()
                }
            } else {
                counterExclude.increment()
            }
        }
    }
}