package no.nav.paw.bekreftelse.plugins


import io.ktor.server.application.Application
import io.ktor.server.application.install
import no.nav.paw.bekreftelse.context.ApplicationContext
import no.nav.paw.kafka.plugin.KafkaStreamsPlugin

fun Application.configureKafka(applicationContext: ApplicationContext) {
    with(applicationContext) {
        install(KafkaStreamsPlugin) {
            kafkaStreams = kafkaStreamsList
        }
    }
}