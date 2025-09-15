package no.nav.paw.kafkakeygenerator.api.v2

import no.nav.paw.kafkakeygenerator.api.models.Identitet
import no.nav.paw.kafkakeygenerator.api.models.IdentitetType

fun no.nav.paw.identitet.internehendelser.vo.Identitet.asApi(): Identitet {
    return Identitet(
        identitet = identitet,
        type = type.asApi(),
        gjeldende = gjeldende
    )
}

fun no.nav.paw.identitet.internehendelser.vo.IdentitetType.asApi(): IdentitetType {
    return when (this) {
        no.nav.paw.identitet.internehendelser.vo.IdentitetType.AKTORID -> IdentitetType.AKTORID
        no.nav.paw.identitet.internehendelser.vo.IdentitetType.ARBEIDSSOEKERID -> IdentitetType.ARBEIDSSOEKERID
        no.nav.paw.identitet.internehendelser.vo.IdentitetType.FOLKEREGISTERIDENT -> IdentitetType.FOLKEREGISTERIDENT
        no.nav.paw.identitet.internehendelser.vo.IdentitetType.NPID -> IdentitetType.NPID
        else -> IdentitetType.UKJENT_VERDI
    }
}
