package no.nav.paw.kafkakeygenerator.model

import no.nav.paw.identitet.internehendelser.vo.Identitet
import no.nav.paw.identitet.internehendelser.vo.IdentitetType
import no.nav.person.pdl.aktor.v2.Type

fun IdentitetRow.asIdentitet(): Identitet = Identitet(
    identitet = identitet,
    type = type,
    gjeldende = gjeldende,
)

fun Type.asIdentitetType(): IdentitetType = when (this) {
    Type.NPID -> IdentitetType.NPID
    Type.AKTORID -> IdentitetType.AKTORID
    Type.FOLKEREGISTERIDENT -> IdentitetType.FOLKEREGISTERIDENT
}

fun Long.asIdentitet(gjeldende: Boolean): Identitet = Identitet(
    identitet = this.toString(),
    type = IdentitetType.ARBEIDSSOEKERID,
    gjeldende = gjeldende,
)
