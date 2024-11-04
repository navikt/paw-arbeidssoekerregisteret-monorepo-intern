package no.nav.paw.kafkakeymaintenance.functions

import no.nav.paw.kafkakeygenerator.client.LokaleAlias
import no.nav.paw.kafkakeymaintenance.vo.Data
import no.nav.person.pdl.aktor.v2.Aktor
import no.nav.person.pdl.aktor.v2.Type
import org.apache.kafka.clients.consumer.ConsumerRecord

fun hentData(
    hentAlias: (List<String>) -> List<LokaleAlias>,
    record: ConsumerRecord<String, Aktor>,
): Data =
    record.value().identifikatorer
        .filter { it.type == Type.FOLKEREGISTERIDENT }
        .map { it.idnummer }
        .let(hentAlias)
        .let { Data(record, it) }