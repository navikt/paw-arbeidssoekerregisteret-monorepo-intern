package no.nav.paw.dolly.api.plugin

import io.ktor.server.application.Application
import io.ktor.server.application.install
import no.nav.paw.dolly.api.context.ApplicationContext
import no.nav.paw.kafka.plugin.KafkaProducerPlugin

fun Application.installKafkaPlugin(applicationContext: ApplicationContext) {
    with(applicationContext) {
        install(KafkaProducerPlugin) {
            kafkaProducers = listOf(kafkaProducer)
        }
    }
}