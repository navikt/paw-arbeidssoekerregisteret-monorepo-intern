package no.nav.paw.kafkakeygenerator.plugin

import io.ktor.server.application.Application
import io.ktor.server.application.install
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.kafka.consumer.CommittingKafkaConsumerWrapper
import no.nav.paw.kafka.handler.ConsumerExceptionHandler
import no.nav.paw.kafka.plugin.KafkaConsumerPlugin
import no.nav.paw.kafkakeygenerator.config.KafkaTopologyConfig
import no.nav.paw.kafkakeygenerator.service.PawHendelseKafkaConsumerService
import org.apache.kafka.clients.consumer.KafkaConsumer

fun Application.installKafkaPlugins(
    kafkaTopologyConfig: KafkaTopologyConfig,
    pawHendelseKafkaConsumer: KafkaConsumer<Long, Hendelse>,
    pawHendelseConsumerExceptionHandler: ConsumerExceptionHandler,
    pawHendelseKafkaConsumerService: PawHendelseKafkaConsumerService
) {
    install(KafkaConsumerPlugin<Long, Hendelse>("Hendelselogg")) {
        this.onConsume = pawHendelseKafkaConsumerService::handleRecords
        this.kafkaConsumerWrapper = CommittingKafkaConsumerWrapper(
            topics = listOf(kafkaTopologyConfig.hendelseloggTopic),
            consumer = pawHendelseKafkaConsumer,
            exceptionHandler = pawHendelseConsumerExceptionHandler
        )
    }
}
