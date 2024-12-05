package no.nav.paw.bekreftelse.api.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.paw.bekreftelse.api.context.ApplicationContext
import no.nav.paw.bekreftelse.api.models.MottaBekreftelseRequest
import no.nav.paw.bekreftelse.api.models.TilgjengeligeBekreftelserRequest
import no.nav.paw.bekreftelse.api.utils.hentSluttbrukerIdentitet
import no.nav.paw.security.authentication.model.asIdentitetsnummer
import no.nav.paw.security.authentication.token.AzureAd
import no.nav.paw.security.authentication.token.IdPorten
import no.nav.paw.security.authentication.token.TokenX
import no.nav.paw.security.authorization.interceptor.authorize
import no.nav.paw.security.authorization.model.Action

fun Route.bekreftelseRoutes(applicationContext: ApplicationContext) {
    val authorizationService = applicationContext.authorizationService
    val bekreftelseService = applicationContext.bekreftelseService

    route("/api/v1") {
        authenticate(IdPorten.name, TokenX.name, AzureAd.name) {
            get("/tilgjengelige-bekreftelser") {
                val accessPolicies = authorizationService.accessPolicies()
                authorize(Action.READ, accessPolicies) { (_, securityContext) ->
                    val (bruker, _) = securityContext
                    val identitetsnummer = bruker.hentSluttbrukerIdentitet()
                    val response = bekreftelseService.finnTilgjengeligBekreftelser(identitetsnummer)
                    call.respond(HttpStatusCode.OK, response)
                }
            }

            post<TilgjengeligeBekreftelserRequest>("/tilgjengelige-bekreftelser") { request ->
                val accessPolicies = authorizationService.accessPolicies(request.identitetsnummer?.asIdentitetsnummer())
                authorize(Action.READ, accessPolicies) { (_, securityContext) ->
                    val (bruker, _) = securityContext
                    val identitetsnummer = bruker.hentSluttbrukerIdentitet(request.identitetsnummer)
                    val response = bekreftelseService.finnTilgjengeligBekreftelser(identitetsnummer)
                    call.respond(HttpStatusCode.OK, response)
                }
            }

            post<MottaBekreftelseRequest>("/bekreftelse") { request ->
                val accessPolicies = authorizationService.accessPolicies(request.identitetsnummer?.asIdentitetsnummer())
                authorize(Action.WRITE, accessPolicies) { (_, securityContext) ->
                    val (bruker, _) = securityContext
                    val identitetsnummer = bruker.hentSluttbrukerIdentitet(request.identitetsnummer)
                    bekreftelseService.mottaBekreftelse(bruker, identitetsnummer, request)
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}
