package no.nav.paw.kafkakeymaintenance.vo

import no.nav.paw.kafkakeygenerator.client.Alias
import no.nav.person.pdl.aktor.v2.Type

fun genererAvviksMelding(data: Data): AvviksMelding {
    return AvviksMelding(
        gjeldeneIdentitetsnummer = data.aktor.identifikatorer
            .filter { it.gjeldende }
            .filter { it.type == Type.FOLKEREGISTERIDENT }
            .map { it.idnummer }
            .firstOrNull(),
        pdlIdentitetsnummer = data.aktor.identifikatorer.map { it.idnummer },
        lokaleAlias = data.alias.flatMap { it.koblinger }
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