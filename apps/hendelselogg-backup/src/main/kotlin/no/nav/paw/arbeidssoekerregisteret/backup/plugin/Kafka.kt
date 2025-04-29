package no.nav.paw.arbeidssoekerregisteret.backup.plugin

import io.ktor.server.application.Application
import io.ktor.server.application.install
import no.nav.paw.arbeidssoekerregisteret.backup.context.ApplicationContext
import no.nav.paw.arbeidssoekerregisteret.backup.health.HealthIndicatorConsumerExceptionHandler
import no.nav.paw.arbeidssoekerregisteret.backup.kafka.HwmRebalanceListener
import no.nav.paw.arbeidssoekerregisteret.backup.kafka.processRecords
import no.nav.paw.arbeidssoekerregisteret.backup.metrics.Metrics
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.health.model.LivenessHealthIndicator
import no.nav.paw.health.model.ReadinessHealthIndicator
import no.nav.paw.kafka.consumer.NonCommittingKafkaConsumerWrapper
import no.nav.paw.kafka.plugin.KafkaConsumerPlugin
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer

fun Application.installKafkaPlugin(
    applicationContext: ApplicationContext,
    hendelseKafkaConsumer: KafkaConsumer<Long, Hendelse>,
) {
    install(KafkaConsumerPlugin<Long, Hendelse>("Hendelseslogg")) {
        val hwmRebalanceListener = HwmRebalanceListener(applicationContext, hendelseKafkaConsumer)
        applicationContext.prometheusMeterRegistry.gauge(
            Metrics.ACTIVE_PARTITIONS_GAUGE,
            hwmRebalanceListener
        ) { it.currentlyAssignedPartitions.size.toDouble() }

        this.onConsume = {
            records: ConsumerRecords<Long, Hendelse> -> processRecords(records, applicationContext)
        }
        this.kafkaConsumerWrapper = NonCommittingKafkaConsumerWrapper(
            rebalanceListener = hwmRebalanceListener,
            topics = listOf(applicationContext.applicationConfig.hendelsesloggTopic),
            consumer = hendelseKafkaConsumer,
            exceptionHandler = HealthIndicatorConsumerExceptionHandler(
                LivenessHealthIndicator(),
                ReadinessHealthIndicator()
            )
        )
    }
}
