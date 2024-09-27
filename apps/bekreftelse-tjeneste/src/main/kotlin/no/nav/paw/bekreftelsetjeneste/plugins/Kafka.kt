package no.nav.paw.bekreftelsetjeneste.plugins


import io.ktor.server.application.*
import org.apache.kafka.streams.KafkaStreams
import java.time.Duration

fun Application.configureKafka(bekreftelseKafkaStreams: KafkaStreams) {
    install(KafkaStreamsPlugin) {
        shutDownTimeout = Duration.ofSeconds(5) // TODO Legg i konfig
        kafkaStreams = listOf(bekreftelseKafkaStreams)
    }
}
