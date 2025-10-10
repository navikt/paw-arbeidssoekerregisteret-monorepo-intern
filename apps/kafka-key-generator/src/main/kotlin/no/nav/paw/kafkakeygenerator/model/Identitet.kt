package no.nav.paw.kafkakeygenerator.model

import no.nav.paw.identitet.internehendelser.vo.Identitet
import no.nav.paw.identitet.internehendelser.vo.IdentitetType
import no.nav.paw.pdl.graphql.generated.enums.IdentGruppe
import no.nav.paw.pdl.graphql.generated.hentidenter.IdentInformasjon
import no.nav.person.pdl.aktor.v2.Identifikator
import no.nav.person.pdl.aktor.v2.Type

fun IdentitetRow.asIdentitet(): Identitet = Identitet(
    identitet = identitet,
    type = type,
    gjeldende = gjeldende,
)

fun KonfliktIdentitetRow.asIdentitet(): Identitet = Identitet(
    identitet = identitet,
    type = type,
    gjeldende = gjeldende,
)

fun Long.asIdentitet(gjeldende: Boolean = true): Identitet = Identitet(
    identitet = this.toString(),
    type = IdentitetType.ARBEIDSSOEKERID,
    gjeldende = gjeldende,
)

fun IdentInformasjon.asIdentitet(): Identitet = Identitet(
    identitet = ident,
    type = gruppe.asIdentitetType(),
    gjeldende = !historisk,
)

fun IdentGruppe.asIdentitetType(): IdentitetType = when (this) {
    IdentGruppe.NPID -> IdentitetType.NPID
    IdentGruppe.AKTORID -> IdentitetType.AKTORID
    IdentGruppe.FOLKEREGISTERIDENT -> IdentitetType.FOLKEREGISTERIDENT
    IdentGruppe.__UNKNOWN_VALUE -> IdentitetType.UKJENT_VERDI
}

fun Identifikator.asIdentitet(): Identitet = Identitet(
    identitet = idnummer,
    type = type.asIdentitetType(),
    gjeldende = gjeldende
)

fun Type.asIdentitetType(): IdentitetType = when (this) {
    Type.NPID -> IdentitetType.NPID
    Type.AKTORID -> IdentitetType.AKTORID
    Type.FOLKEREGISTERIDENT -> IdentitetType.FOLKEREGISTERIDENT
}
