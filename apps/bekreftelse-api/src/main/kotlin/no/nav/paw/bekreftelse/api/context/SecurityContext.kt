package no.nav.paw.bekreftelse.api.context

import no.nav.paw.bekreftelse.api.model.AccessToken
import no.nav.paw.bekreftelse.api.model.InnloggetBruker
import no.nav.paw.bekreftelse.api.model.Sluttbruker
import no.nav.poao_tilgang.client.TilgangType

data class SecurityContext(
    val sluttbruker: Sluttbruker,
    val innloggetBruker: InnloggetBruker,
    val accessToken: AccessToken,
    val tilgangType: TilgangType
)
