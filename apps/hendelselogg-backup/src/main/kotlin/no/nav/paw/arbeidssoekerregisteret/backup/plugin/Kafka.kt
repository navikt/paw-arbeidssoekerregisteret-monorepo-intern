package no.nav.paw.arbeidssoekerregisteret.backup.plugin

import io.ktor.server.application.Application
import io.ktor.server.application.install
import no.nav.paw.arbeidssoekerregisteret.backup.context.ApplicationContext
import no.nav.paw.arbeidssoekerregisteret.backup.health.HealthIndicatorConsumerExceptionHandler
import no.nav.paw.arbeidssoekerregisteret.backup.kafka.HwmRebalanceListener
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.health.model.LivenessHealthIndicator
import no.nav.paw.health.model.ReadinessHealthIndicator
import no.nav.paw.kafka.consumer.NonCommittingKafkaConsumerWrapper
import no.nav.paw.kafka.plugin.KafkaConsumerPlugin
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer

fun Application.installKafkaConsumerPlugin(
    applicationContext: ApplicationContext,
    hendelseKafkaConsumer: KafkaConsumer<Long, Hendelse>,
    consumeFunction: ((ConsumerRecords<Long, Hendelse>) -> Unit)
) = with(applicationContext) {
    install(KafkaConsumerPlugin<Long, Hendelse>("Hendelseslogg")) {
        val hwmRebalanceListener = HwmRebalanceListener(applicationConfig.consumerVersion, hendelseKafkaConsumer)
        metrics.createActivePartitionsGauge(hwmRebalanceListener)
        this.onConsume = consumeFunction
        this.kafkaConsumerWrapper = NonCommittingKafkaConsumerWrapper(
            rebalanceListener = hwmRebalanceListener,
            topics = listOf(applicationConfig.hendelsesloggTopic),
            consumer = hendelseKafkaConsumer,
            exceptionHandler = HealthIndicatorConsumerExceptionHandler(
                LivenessHealthIndicator(),
                ReadinessHealthIndicator()
            )
        )
    }
}
