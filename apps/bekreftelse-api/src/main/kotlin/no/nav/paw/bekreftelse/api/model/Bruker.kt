package no.nav.paw.bekreftelse.api.model

import java.util.*

enum class BrukerType {
    SLUTTBRUKER,
    VEILEDER
}

data class InnloggetBruker(val type: BrukerType, val ident: String, val bearerToken: String)

data class Sluttbruker(
    val identitetsnummer: String,
    val arbeidssoekerId: Long,
    val kafkaKey: Long
)

fun BrukerType.toApi(): no.nav.paw.bekreftelse.melding.v1.vo.BrukerType {
    return when (this) {
        BrukerType.SLUTTBRUKER -> no.nav.paw.bekreftelse.melding.v1.vo.BrukerType.SLUTTBRUKER
        BrukerType.VEILEDER -> no.nav.paw.bekreftelse.melding.v1.vo.BrukerType.VEILEDER
    }
}

data class NavAnsatt(val azureId: UUID, val navIdent: String)