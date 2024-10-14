package no.nav.paw.bekreftelse.api.plugins


import io.ktor.server.application.Application
import io.ktor.server.application.install
import no.nav.paw.bekreftelse.api.context.ApplicationContext
import no.nav.paw.bekreftelse.api.plugins.custom.KafkaConsumerPlugin
import no.nav.paw.bekreftelse.api.plugins.custom.KafkaProducerPlugin

fun Application.configureKafka(applicationContext: ApplicationContext) {
    with(applicationContext) {
        install(KafkaProducerPlugin) {
            kafkaProducers = listOf(bekreftelseKafkaProducer)
        }
        install(KafkaConsumerPlugin) {
            consumeFunction = bekreftelseService::processBekreftelseHendelse
            errorFunction = kafkaConsumerExceptionHandler::handleException
            consumer = bekreftelseKafkaConsumer
            topic = applicationConfig.kafkaTopology.bekreftelseHendelsesloggTopic
        }
    }
}
