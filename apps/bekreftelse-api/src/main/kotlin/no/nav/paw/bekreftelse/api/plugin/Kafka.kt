package no.nav.paw.bekreftelse.api.plugin


import io.ktor.server.application.Application
import io.ktor.server.application.install
import no.nav.paw.bekreftelse.api.service.BekreftelseService
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import no.nav.paw.kafka.consumer.KafkaConsumerWrapper
import no.nav.paw.kafka.plugin.KafkaConsumerPlugin
import no.nav.paw.kafka.plugin.KafkaProducerPlugin
import org.apache.kafka.clients.producer.Producer

fun Application.installKafkaPlugins(
    bekreftelseKafkaProducer: Producer<Long, Bekreftelse>,
    bekreftelseHendelseKafkaConsumerWrapper: KafkaConsumerWrapper<Long, BekreftelseHendelse>,
    bekreftelseService: BekreftelseService
) {
    install(KafkaProducerPlugin) {
        kafkaProducers = listOf(bekreftelseKafkaProducer)
    }
    install(KafkaConsumerPlugin<Long, BekreftelseHendelse>("BekreftelseHendelser")) {
        this.onConsume = bekreftelseService::processBekreftelseHendelser
        this.kafkaConsumerWrapper = bekreftelseHendelseKafkaConsumerWrapper
    }
}