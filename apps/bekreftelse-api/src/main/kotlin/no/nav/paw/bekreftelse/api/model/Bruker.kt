package no.nav.paw.bekreftelse.api.model

import no.nav.paw.bekreftelse.melding.v1.vo.Bruker
import java.util.*

enum class BrukerType {
    UKJENT_VERDI,
    SLUTTBRUKER,
    VEILEDER
}

data class InnloggetBruker(val type: BrukerType, val ident: String)

data class Sluttbruker(
    val identitetsnummer: String,
    val arbeidssoekerId: Long,
    val kafkaKey: Long
)

fun InnloggetBruker.asBruker(): Bruker {
    return Bruker.newBuilder()
        .setId(ident)
        .setType(type.asBrukerType())
        .build()
}

fun BrukerType.asBrukerType(): no.nav.paw.bekreftelse.melding.v1.vo.BrukerType {
    return when (this) {
        BrukerType.SLUTTBRUKER -> no.nav.paw.bekreftelse.melding.v1.vo.BrukerType.SLUTTBRUKER
        BrukerType.VEILEDER -> no.nav.paw.bekreftelse.melding.v1.vo.BrukerType.VEILEDER
        else -> no.nav.paw.bekreftelse.melding.v1.vo.BrukerType.UKJENT_VERDI
    }
}

data class NavAnsatt(val azureId: UUID, val navIdent: String)