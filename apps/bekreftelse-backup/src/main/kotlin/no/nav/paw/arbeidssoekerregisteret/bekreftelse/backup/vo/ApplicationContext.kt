package no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.vo

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.Logger

data class ApplicationContext(
    val consumerVersion: Int,
    val logger: Logger,
    val meterRegistry: PrometheusMeterRegistry,
    val hendelseConsumer: KafkaConsumer<Long, BekreftelseHendelse>,
    val bekreftelseConsumer: KafkaConsumer<Long, ByteArray>,
    val paaVegneAvConsumer: KafkaConsumer<Long, ByteArray>,
    val hendelseTopic: String,
    val bekreftelseTopic: String,
    val paaVegneAvTopic: String,
)