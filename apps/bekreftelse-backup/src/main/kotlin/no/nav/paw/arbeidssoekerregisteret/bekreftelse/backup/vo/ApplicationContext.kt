package no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.vo

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.Logger
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.BEKREFTELSE_HENDELSE_TOPIC
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.BEKREFTELSE_TOPIC
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.BEKREFTELSE_PAA_VEGNE_AV_TOPIC

data class ApplicationContext(
    val consumerVersion: Int,
    val logger: Logger,
    val meterRegistry: PrometheusMeterRegistry,
    val hendelseConsumer: KafkaConsumer<Long, BekreftelseHendelse>,
    val bekreftelseConsumer: KafkaConsumer<Long, ByteArray>,
    val paaVegneAvConsumer: KafkaConsumer<Long, ByteArray>,
) {
    val hendelseTopic: String = BEKREFTELSE_HENDELSE_TOPIC
    val bekreftelseTopic: String = BEKREFTELSE_TOPIC
    val paaVegneAvTopic: String = BEKREFTELSE_PAA_VEGNE_AV_TOPIC
}
