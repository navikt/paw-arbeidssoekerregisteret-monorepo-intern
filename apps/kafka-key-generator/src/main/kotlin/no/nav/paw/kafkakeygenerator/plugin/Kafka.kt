package no.nav.paw.kafkakeygenerator.plugin

import io.ktor.server.application.Application
import io.ktor.server.application.install
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.kafka.consumer.CommittingKafkaConsumerWrapper
import no.nav.paw.kafka.consumer.NonCommittingKafkaConsumerWrapper
import no.nav.paw.kafka.handler.ConsumerExceptionHandler
import no.nav.paw.kafka.plugin.KafkaConsumerPlugin
import no.nav.paw.kafkakeygenerator.config.ApplicationConfig
import no.nav.paw.kafkakeygenerator.listener.HwmConsumerRebalanceListener
import no.nav.paw.kafkakeygenerator.service.PawHendelseKafkaConsumerService
import no.nav.paw.kafkakeygenerator.service.PawPeriodeKafkaConsumerService
import no.nav.paw.kafkakeygenerator.service.PdlAktorKafkaConsumerService
import no.nav.person.pdl.aktor.v2.Aktor
import org.apache.kafka.clients.consumer.KafkaConsumer

fun Application.installKafkaPlugins(
    applicationConfig: ApplicationConfig,
    pawHendelseConsumer: KafkaConsumer<Long, Hendelse>,
    pawHendelseConsumerExceptionHandler: ConsumerExceptionHandler,
    pawHendelseKafkaConsumerService: PawHendelseKafkaConsumerService,
    pawPeriodeConsumer: KafkaConsumer<Long, Periode>,
    pawPeriodeConsumerExceptionHandler: ConsumerExceptionHandler,
    pawPeriodeHwmRebalanceListener: HwmConsumerRebalanceListener,
    pawPeriodeKafkaConsumerService: PawPeriodeKafkaConsumerService,
    pdlAktorConsumer: KafkaConsumer<Any, Aktor>,
    pdlAktorConsumerExceptionHandler: ConsumerExceptionHandler,
    pdlAktorHwmRebalanceListener: HwmConsumerRebalanceListener,
    pdlAktorKafkaConsumerService: PdlAktorKafkaConsumerService
) {
    install(KafkaConsumerPlugin<Long, Hendelse>("PawHendelselogg")) {
        this.onConsume = pawHendelseKafkaConsumerService::handleRecords
        this.kafkaConsumerWrapper = CommittingKafkaConsumerWrapper(
            topics = listOf(applicationConfig.pawHendelseConsumer.topic),
            consumer = pawHendelseConsumer,
            exceptionHandler = pawHendelseConsumerExceptionHandler
        )
    }
    install(KafkaConsumerPlugin<Long, Periode>("PawPeriode")) {
        this.onInit = pawPeriodeHwmRebalanceListener::onPartitionsReady
        this.onConsume = pawPeriodeKafkaConsumerService::handleRecords
        this.kafkaConsumerWrapper = NonCommittingKafkaConsumerWrapper(
            topics = listOf(applicationConfig.pawPeriodeConsumer.topic),
            consumer = pawPeriodeConsumer,
            exceptionHandler = pawPeriodeConsumerExceptionHandler,
            rebalanceListener = pawPeriodeHwmRebalanceListener
        )
    }
    install(KafkaConsumerPlugin<Any, Aktor>("PdlAktor")) {
        this.onInit = pdlAktorHwmRebalanceListener::onPartitionsReady
        this.onConsume = pdlAktorKafkaConsumerService::handleRecords
        this.kafkaConsumerWrapper = NonCommittingKafkaConsumerWrapper(
            topics = listOf(applicationConfig.pdlAktorConsumer.topic),
            consumer = pdlAktorConsumer,
            exceptionHandler = pdlAktorConsumerExceptionHandler,
            rebalanceListener = pdlAktorHwmRebalanceListener
        )
    }
}
