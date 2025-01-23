package no.nav.paw.kafkakeymaintenance.vo

import no.nav.paw.kafkakeygenerator.client.Alias
import no.nav.paw.kafkakeymaintenance.pdlprocessor.lagring.IdentType

fun genererAvviksMelding(data: Data): AvviksMelding {
    return AvviksMelding(
        gjeldeneIdentitetsnummer = data.pdlIdentiteter
            .filter { it.gjeldende }
            .filter { it.identType == IdentType.FOLKEREGISTERET }
            .map { it.ident }
            .firstOrNull(),
        pdlIdentitetsnummer = data.pdlIdentiteter.map { it.ident },
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