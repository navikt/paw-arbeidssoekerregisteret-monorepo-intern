package no.nav.paw.kafkakeygenerator.plugin

import io.ktor.server.application.Application
import io.ktor.server.application.install
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.kafka.consumer.NonCommittingKafkaConsumerWrapper
import no.nav.paw.kafka.handler.ConsumerExceptionHandler
import no.nav.paw.kafka.listener.HwmConsumerRebalanceListener
import no.nav.paw.kafka.plugin.KafkaConsumerPlugin
import no.nav.paw.kafkakeygenerator.config.ApplicationConfig
import no.nav.paw.kafkakeygenerator.client.PawPeriodeKafkaConsumer
import no.nav.paw.kafkakeygenerator.client.PdlAktorKafkaConsumer
import no.nav.person.pdl.aktor.v2.Aktor
import org.apache.kafka.clients.consumer.KafkaConsumer

fun Application.installKafkaPlugins(
    applicationConfig: ApplicationConfig,
    pawPeriodeConsumer: KafkaConsumer<Long, Periode>,
    pawPeriodeConsumerExceptionHandler: ConsumerExceptionHandler,
    pawPeriodeHwmRebalanceListener: HwmConsumerRebalanceListener,
    pawPeriodeKafkaConsumer: PawPeriodeKafkaConsumer,
    pdlAktorConsumer: KafkaConsumer<Any, Aktor>,
    pdlAktorConsumerExceptionHandler: ConsumerExceptionHandler,
    pdlAktorHwmRebalanceListener: HwmConsumerRebalanceListener,
    pdlAktorKafkaConsumer: PdlAktorKafkaConsumer
) {
    install(KafkaConsumerPlugin<Long, Periode>("PawPeriode")) {
        this.onInit = pawPeriodeHwmRebalanceListener::onPartitionsReady
        this.onConsume = pawPeriodeKafkaConsumer::handleRecords
        this.kafkaConsumerWrapper = NonCommittingKafkaConsumerWrapper(
            topics = listOf(applicationConfig.pawPeriodeConsumer.topic),
            consumer = pawPeriodeConsumer,
            exceptionHandler = pawPeriodeConsumerExceptionHandler,
            rebalanceListener = pawPeriodeHwmRebalanceListener
        )
    }
    install(KafkaConsumerPlugin<Any, Aktor>("PdlAktor")) {
        this.onInit = pdlAktorHwmRebalanceListener::onPartitionsReady
        this.onConsume = pdlAktorKafkaConsumer::handleRecords
        this.kafkaConsumerWrapper = NonCommittingKafkaConsumerWrapper(
            topics = listOf(applicationConfig.pdlAktorConsumer.topic),
            consumer = pdlAktorConsumer,
            exceptionHandler = pdlAktorConsumerExceptionHandler,
            rebalanceListener = pdlAktorHwmRebalanceListener
        )
    }
}
