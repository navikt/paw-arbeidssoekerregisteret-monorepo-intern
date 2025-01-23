package no.nav.paw.kafkakeymaintenance.pdlprocessor

import no.nav.paw.kafkakeymaintenance.pdlprocessor.lagring.Ident
import no.nav.paw.kafkakeymaintenance.pdlprocessor.lagring.IdentType
import no.nav.paw.kafkakeymaintenance.pdlprocessor.lagring.identRad
import no.nav.person.pdl.aktor.v2.Aktor
import no.nav.person.pdl.aktor.v2.Type

fun Type.tilInternType(): IdentType = when (this) {
    Type.FOLKEREGISTERIDENT -> IdentType.FOLKEREGISTERET
    Type.AKTORID -> IdentType.AKTORID
    Type.NPID -> IdentType.NPID
}

fun tilIdentRader(
    aktor: Aktor
): List<Ident> = aktor.identifikatorer.map { ident ->
    identRad(
        ident = ident.idnummer,
        identType = ident.type.tilInternType(),
        gjeldende = ident.gjeldende
    )
}
