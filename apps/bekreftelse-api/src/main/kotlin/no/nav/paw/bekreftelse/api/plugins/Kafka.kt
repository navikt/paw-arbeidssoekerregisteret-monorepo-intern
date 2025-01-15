package no.nav.paw.bekreftelse.api.plugins


import io.ktor.server.application.Application
import io.ktor.server.application.install
import no.nav.paw.bekreftelse.api.context.ApplicationContext
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.kafka.plugin.KafkaConsumerPlugin
import no.nav.paw.kafka.plugin.KafkaProducerPlugin

fun Application.configureKafka(applicationContext: ApplicationContext) {
    with(applicationContext) {
        install(KafkaProducerPlugin) {
            kafkaProducers = listOf(bekreftelseKafkaProducer)
        }
        install(KafkaConsumerPlugin<Long, BekreftelseHendelse>("BekreftelseHendelser")) {
            this.consumeFunction = bekreftelseService::processBekreftelseHendelser
            this.errorFunction = kafkaConsumerExceptionHandler::handleException
            this.kafkaConsumer = bekreftelseKafkaConsumer
            this.kafkaTopics = listOf(applicationConfig.kafkaTopology.bekreftelseHendelsesloggTopic)
        }
    }
}