package no.nav.paw.kafkakeymaintenance.vo

import no.nav.paw.kafkakeygenerator.client.LokaleAlias
import no.nav.paw.kafkakeymaintenance.pdlprocessor.lagring.Ident

data class Data(
    val pdlIdentiteter: List<Ident>,
    val alias: List<LokaleAlias>
)

fun Data.debugString(): String {
    val pdlIdenterEtterType = pdlIdentiteter
        .groupBy { it.identType }
        .mapValues { kv -> kv.value.size }
    val aliasString = alias
        .flatMap { it.koblinger }
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