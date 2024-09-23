package no.nav.paw.bekreftelse.api.context

import no.nav.paw.bekreftelse.api.authz.AccessToken
import no.nav.paw.bekreftelse.api.model.InnloggetBruker
import no.nav.paw.bekreftelse.api.model.Sluttbruker

sealed class NavHeader(val name: String)

data object TraceParent : NavHeader("traceparent")
data object NavCallId : NavHeader("Nav-Call-Id")
data object NavConsumerId : NavHeader("Nav-Consumer-Id")

data class RequestContext(
    val path: String,
    val callId: String?,
    val traceparent: String?,
    val navConsumerId: String?,
    val useMockData: Boolean,
    val sluttbruker: Sluttbruker,
    val innloggetBruker: InnloggetBruker,
    val accessToken: AccessToken
)
