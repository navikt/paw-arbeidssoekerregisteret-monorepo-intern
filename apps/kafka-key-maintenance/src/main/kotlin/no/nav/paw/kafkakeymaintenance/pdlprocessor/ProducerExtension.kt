package no.nav.paw.kafkakeymaintenance.pdlprocessor

import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.kafkakeymaintenance.kafka.Topic
import no.nav.paw.kafkakeymaintenance.pdlprocessor.functions.HendelseRecord
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import java.util.concurrent.Future

fun Producer<Long, Hendelse>.sendSync(topic: Topic, hendelseRecord: HendelseRecord<Hendelse>) {
    send(hendelseRecord.toProducerRecord(topic)).get()
}

fun Producer<Long, Hendelse>.send(topic: Topic, hendelseRecord: HendelseRecord<Hendelse>): Future<RecordMetadata> {
    return send(hendelseRecord.toProducerRecord(topic))
}

fun HendelseRecord<Hendelse>.toProducerRecord(hendelseTopic: Topic): ProducerRecord<Long, Hendelse> {
    return ProducerRecord(
        hendelseTopic.value,
        key,
        hendelse
    )
}
