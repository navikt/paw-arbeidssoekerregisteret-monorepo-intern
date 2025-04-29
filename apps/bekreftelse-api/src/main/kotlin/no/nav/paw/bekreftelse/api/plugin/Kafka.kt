package no.nav.paw.bekreftelse.api.plugin


import io.ktor.server.application.Application
import io.ktor.server.application.install
import no.nav.paw.bekreftelse.api.config.ApplicationConfig
import no.nav.paw.bekreftelse.api.service.BekreftelseService
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import no.nav.paw.kafka.consumer.CommittingKafkaConsumerWrapper
import no.nav.paw.kafka.handler.ConsumerExceptionHandler
import no.nav.paw.kafka.plugin.KafkaConsumerPlugin
import no.nav.paw.kafka.plugin.KafkaProducerPlugin
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.Producer

fun Application.installKafkaPlugins(
    applicationConfig: ApplicationConfig,
    bekreftelseKafkaProducer: Producer<Long, Bekreftelse>,
    bekreftelseHendelseKafkaConsumer: KafkaConsumer<Long, BekreftelseHendelse>,
    bekreftelseService: BekreftelseService,
    consumerExceptionHandler: ConsumerExceptionHandler
) {
    install(KafkaProducerPlugin) {
        kafkaProducers = listOf(bekreftelseKafkaProducer)
    }
    install(KafkaConsumerPlugin<Long, BekreftelseHendelse>("BekreftelseHendelser")) {
        this.onConsume = bekreftelseService::processBekreftelseHendelser
        this.kafkaConsumerWrapper = CommittingKafkaConsumerWrapper(
            topics = listOf(applicationConfig.kafkaTopology.bekreftelseHendelsesloggTopic),
            consumer = bekreftelseHendelseKafkaConsumer,
            exceptionHandler = consumerExceptionHandler
        )
    }
}