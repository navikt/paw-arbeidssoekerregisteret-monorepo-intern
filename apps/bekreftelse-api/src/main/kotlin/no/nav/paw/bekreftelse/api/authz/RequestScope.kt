package no.nav.paw.bekreftelse.api.authz

import io.ktor.http.HttpHeaders
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.principal
import io.ktor.server.request.path
import io.ktor.util.pipeline.PipelineContext
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.bekreftelse.api.exception.BearerTokenManglerException
import no.nav.paw.bekreftelse.api.exception.BrukerHarIkkeTilgangException
import no.nav.paw.bekreftelse.api.exception.UfullstendigBearerTokenException
import no.nav.paw.bekreftelse.api.exception.UkjentBearerTokenException
import no.nav.paw.bekreftelse.api.model.BrukerType
import no.nav.paw.bekreftelse.api.model.InnloggetBruker
import no.nav.paw.bekreftelse.api.model.Sluttbruker
import no.nav.paw.bekreftelse.api.services.AutorisasjonService
import no.nav.paw.kafkakeygenerator.client.KafkaKeysResponse
import no.nav.poao_tilgang.client.TilgangType
import no.nav.security.token.support.v2.TokenValidationContextPrincipal

data class RequestScope(
    val sluttbruker: Sluttbruker,
    val innloggetBruker: InnloggetBruker,
    val token: Token,
    val path: String,
    val callId: String?,
    val traceparent: String?,
    val navConsumerId: String?,
    val useMockData: Boolean
)

@Suppress("ConstPropertyName")
object NavHttpHeaders {
    const val TraceParent = "traceparent"
    const val NavCallId = "Nav-Call-Id"
    const val NavConsumerId = "Nav-Consumer-Id"
}

@WithSpan
suspend fun PipelineContext<Unit, ApplicationCall>.requestScope(
    identitetsnummer: String?,
    kafkaKeysFunctions: suspend (ident: String) -> KafkaKeysResponse,
    autorisasjonService: AutorisasjonService, // TODO Legg til autorisasjon
    tilgangType: TilgangType
): RequestScope {
    val bearerToken = call.request.headers[HttpHeaders.Authorization]
        ?: throw BearerTokenManglerException("Request mangler Bearer Token")

    val principal = call.principal<TokenValidationContextPrincipal>()
    val token = principal
        ?.context
        ?.resolveTokens()
        ?: throw UkjentBearerTokenException("Fant ikke token med forventet issuer")

    if (token.claims.isEmpty()) {
        throw UfullstendigBearerTokenException("Bearer Token mangler påkrevd innhold")
    }

    if (!token.isValidIssuer()) {
        throw UkjentBearerTokenException("Bearer Token er utstedt av ukjent issuer")
    }

    val sluttbrukerIdentitetsnummer = when (token.issuer) {
        is Azure -> {
            // TODO Gjøre sjekk mot POAO Tilgang at veileder kan behandle sluttbruker
            // Veiledere skal alltid sende inn identitetsnummer for sluttbruker
            identitetsnummer
                ?: throw BrukerHarIkkeTilgangException("Veileder må sende med identitetsnummer for sluttbruker")
        }

        else -> {
            val pid = token[PID].verdi
            if (identitetsnummer != null && identitetsnummer != pid) {
                // TODO Håndtere verge
                throw BrukerHarIkkeTilgangException("Bruker har ikke tilgang til sluttbrukers informasjon")
            }
            identitetsnummer ?: pid
        }
    }

    val kafkaKeysResponse = kafkaKeysFunctions(sluttbrukerIdentitetsnummer)

    val sluttbruker = Sluttbruker(
        identitetsnummer = sluttbrukerIdentitetsnummer,
        arbeidssoekerId = kafkaKeysResponse.id,
        kafkaKey = kafkaKeysResponse.key
    )

    val innloggetBruker = when (token.issuer) {
        is Azure -> {
            val ident = token[NavIdent]
            InnloggetBruker(
                type = BrukerType.VEILEDER,
                ident = ident,
                bearerToken = bearerToken
            )
        }

        else -> {
            val ident = token[PID].verdi
            InnloggetBruker(
                type = BrukerType.SLUTTBRUKER,
                ident = ident,
                bearerToken = bearerToken
            )
        }
    }

    return RequestScope(
        sluttbruker = sluttbruker,
        innloggetBruker = innloggetBruker,
        token = token,
        path = call.request.path(),
        callId = call.request.headers[NavHttpHeaders.NavCallId],
        traceparent = call.request.headers[NavHttpHeaders.TraceParent],
        navConsumerId = call.request.headers[NavHttpHeaders.NavConsumerId],
        useMockData = call.request.queryParameters["useMockData"].toBoolean()
    )
}
