package no.nav.paw.bekreftelse.api.plugins


import io.ktor.server.application.Application
import io.ktor.server.application.install
import no.nav.paw.bekreftelse.api.context.ApplicationContext
import java.time.Duration

fun Application.configureKafka(applicationContext: ApplicationContext) {
    install(KafkaStreamsPlugin) {
        shutDownTimeout = Duration.ofSeconds(5) // TODO Legg i konfig
        kafkaStreams = listOf(applicationContext.bekreftelseKafkaStreams)
    }
}
