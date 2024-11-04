package no.nav.paw.kafkakeymaintenance.vo

import no.nav.paw.kafkakeygenerator.client.LokaleAlias
import no.nav.person.pdl.aktor.v2.Aktor
import org.apache.kafka.clients.consumer.ConsumerRecord

data class Data(
    val record: ConsumerRecord<String, Aktor>,
    val alias: List<LokaleAlias>
)