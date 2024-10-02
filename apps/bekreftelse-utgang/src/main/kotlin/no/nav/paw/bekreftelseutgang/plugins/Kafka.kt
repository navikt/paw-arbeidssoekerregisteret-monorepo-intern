package no.nav.paw.bekreftelseutgang.plugins


import io.ktor.server.application.Application
import io.ktor.server.application.install
import no.nav.paw.bekreftelseutgang.context.ApplicationContext
import org.apache.kafka.streams.KafkaStreams

fun Application.configureKafka(
    applicationContext: ApplicationContext,
    kafkaStreams: KafkaStreams
) {
    install(KafkaStreamsPlugin) {
        shutDownTimeout = applicationContext.applicationConfig.kafkaTopology.shutdownTimeout
        kafkaStreamsList = listOf(kafkaStreams)
    }
}
