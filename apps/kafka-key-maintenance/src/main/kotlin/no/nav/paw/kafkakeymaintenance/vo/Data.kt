package no.nav.paw.kafkakeymaintenance.vo

import no.nav.paw.kafkakeygenerator.client.LokaleAlias
import no.nav.person.pdl.aktor.v2.Aktor
import org.apache.kafka.clients.consumer.ConsumerRecord

data class Data(
    val aktor: Aktor,
    val alias: List<LokaleAlias>
)

fun Data.debugString(): String {
    val pdlIdenterEtterType = aktor.identifikatorer
        .groupBy { it.type }
        .mapValues { kv -> kv.value.size }
    val aliasString = alias
        .flatMap { it.kobliner }
        .groupBy { it.arbeidsoekerId }
        .toList()
        .sortedBy { it.first}
        .map { it.second }
        .map { it.size }
        .mapIndexed { index, antall ->
            "arbeidssøkerId$index: $antall"
        }.joinToString(", ")
    return "Pdl identer: $pdlIdenterEtterType, antall identitetsnummer per arbeidssøkerId: $aliasString"
}