package no.nav.paw.bekreftelse.api.plugins


import io.ktor.server.application.Application
import io.ktor.server.application.install
import no.nav.paw.bekreftelse.api.config.ApplicationConfig
import org.apache.kafka.streams.KafkaStreams
import java.time.Duration

fun Application.configureKafka(
    applicationConfig: ApplicationConfig,
    kafkaStreamsList: List<KafkaStreams>
) {
    install(KafkaStreamsPlugin) {
        shutDownTimeout = Duration.ofSeconds(5) // TODO Legg i konfig
        kafkaStreams = kafkaStreamsList
    }
}
