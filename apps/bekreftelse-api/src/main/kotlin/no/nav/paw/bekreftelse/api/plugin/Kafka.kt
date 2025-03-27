package no.nav.paw.bekreftelse.api.plugin


import io.ktor.server.application.Application
import io.ktor.server.application.install
import no.nav.paw.bekreftelse.api.config.ApplicationConfig
import no.nav.paw.bekreftelse.api.handler.KafkaConsumerHandler
import no.nav.paw.bekreftelse.api.service.BekreftelseService
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import no.nav.paw.kafka.plugin.KafkaConsumerPlugin
import no.nav.paw.kafka.plugin.KafkaProducerPlugin
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.Producer

fun Application.installKafkaPlugins(
    applicationConfig: ApplicationConfig,
    bekreftelseKafkaProducer: Producer<Long, Bekreftelse>,
    bekreftelseHendelseKafkaConsumer: KafkaConsumer<Long, BekreftelseHendelse>,
    bekreftelseService: BekreftelseService,
    kafkaConsumerHandler: KafkaConsumerHandler
) {
    install(KafkaProducerPlugin) {
        kafkaProducers = listOf(bekreftelseKafkaProducer)
    }
    install(KafkaConsumerPlugin<Long, BekreftelseHendelse>("BekreftelseHendelser")) {
        this.consumeFunction = bekreftelseService::processBekreftelseHendelser
        this.errorFunction = kafkaConsumerHandler::handleException
        this.kafkaConsumer = bekreftelseHendelseKafkaConsumer
        this.kafkaTopics = listOf(applicationConfig.kafkaTopology.bekreftelseHendelsesloggTopic)
    }
}