package no.nav.paw.arbeidssoekerregisteret.backup.plugin

import io.ktor.server.application.Application
import io.ktor.server.application.install
import no.nav.paw.arbeidssoekerregisteret.backup.context.ApplicationContext
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.kafka.plugin.KafkaConsumerPlugin
import org.apache.kafka.clients.consumer.ConsumerRecords

fun Application.installKafkaConsumerPlugin(
    applicationContext: ApplicationContext,
    consumeFunction: ((ConsumerRecords<Long, Hendelse>) -> Unit),
) = install(KafkaConsumerPlugin<Long, Hendelse>("Hendelseslogg")) {
    applicationContext.metrics.createActivePartitionsGauge(applicationContext.hwmRebalanceListener)
    this.onConsume = consumeFunction
    this.kafkaConsumerWrapper = applicationContext.hendelseConsumerWrapper
}

