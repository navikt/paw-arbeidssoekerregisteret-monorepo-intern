package no.nav.paw.kafkakeymaintenance.vo

import no.nav.paw.kafkakeygenerator.client.Alias
import no.nav.person.pdl.aktor.v2.Type

fun avviksMelding(data: Data): AvviksMelding {
    val fregIder = data.record.value()
        .identifikatorer
        .filter { it.type == Type.FOLKEREGISTERIDENT }
    return AvviksMelding(
        gjeldeneIdentitetsnummer = fregIder
            .filter { it.gjeldende }
            .map { it.idnummer }
            .firstOrNull(),
        pdlIdentitetsnummer = fregIder.map { it.idnummer },
        lokaleAlias = data.alias.flatMap { it.kobliner }
    )
}

data class AvviksMelding(
    val gjeldeneIdentitetsnummer: String?,
    val pdlIdentitetsnummer: List<String>,
    val lokaleAlias: List<Alias>
) {
    fun lokaleAliasSomSkalPekePaaPdlPerson() = lokaleAlias.filter { it.identitetsnummer in pdlIdentitetsnummer }

    fun lokaleAliasSomIkkeSkalPekePaaPdlPerson() = lokaleAlias.filter { it.identitetsnummer !in pdlIdentitetsnummer }
}