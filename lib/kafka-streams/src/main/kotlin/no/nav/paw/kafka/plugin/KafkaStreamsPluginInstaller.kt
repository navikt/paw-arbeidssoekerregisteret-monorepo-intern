package no.nav.paw.kafka.plugin

import io.ktor.server.application.*
import org.apache.kafka.streams.KafkaStreams
import java.time.Duration

fun Application.installKafkaStreamsPlugins(
    kafkaStreamsList: List<KafkaStreams>,
    shutDownTimeout: Duration = Duration.ofSeconds(1)
) {
    install(KafkaStreamsPlugin) {
        this.kafkaStreams = kafkaStreamsList
        this.shutDownTimeout = shutDownTimeout
    }
}
