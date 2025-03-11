package no.nav.paw.arbeidssoekerregisteret.utils

import kotlinx.coroutines.runBlocking
import no.nav.paw.arbeidssoekerregisteret.model.OpprettBeskjed
import no.nav.paw.arbeidssoekerregisteret.model.VarselHendelse
import no.nav.paw.kafka.producer.sendDeferred
import no.nav.paw.logging.logger.buildNamedLogger
import no.nav.paw.serialization.kafka.JacksonSerde
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord

private val logger = buildNamedLogger("kafka.producer")

class VarselHendelseJsonSerde : JacksonSerde<VarselHendelse>(VarselHendelse::class)

fun Producer<String, String>.sendVarsel(topic: String, varsel: OpprettBeskjed) =
    sendRecord(topic, varsel.varselId.toString(), varsel.value)

fun <K, V> Producer<K, V>.sendRecord(topic: String, key: K, value: V) = runBlocking {
    val metadata = sendDeferred(ProducerRecord(topic, key, value))
        .await()
    logger.debug(
        "Sender melding til Kafka topic {} (partition={}, offset={})",
        topic,
        metadata.partition(),
        metadata.offset()
    )
}
