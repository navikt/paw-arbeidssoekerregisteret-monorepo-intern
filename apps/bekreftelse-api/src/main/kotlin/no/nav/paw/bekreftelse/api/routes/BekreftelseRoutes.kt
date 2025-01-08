package no.nav.paw.bekreftelse.api.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.paw.bekreftelse.api.context.ApplicationContext
import no.nav.paw.bekreftelse.api.models.MottaBekreftelseRequest
import no.nav.paw.bekreftelse.api.models.TilgjengeligeBekreftelserRequest
import no.nav.paw.bekreftelse.api.utils.hentSluttbrukerIdentitet
import no.nav.paw.security.authentication.interceptor.autentisering
import no.nav.paw.security.authentication.model.AzureAd
import no.nav.paw.security.authentication.model.Bruker
import no.nav.paw.security.authentication.model.IdPorten
import no.nav.paw.security.authentication.model.TokenX
import no.nav.paw.security.authentication.model.asIdentitetsnummer
import no.nav.paw.security.authentication.model.bruker
import no.nav.paw.security.authorization.interceptor.autorisering
import no.nav.paw.security.authorization.model.Action

fun Route.bekreftelseRoutes(applicationContext: ApplicationContext) {
    val authorizationService = applicationContext.authorizationService
    val bekreftelseService = applicationContext.bekreftelseService

    route("/api/v1") {
        autentisering(IdPorten, TokenX, AzureAd) {
            get("/tilgjengelige-bekreftelser") {
                val accessPolicies = authorizationService.accessPolicies()
                autorisering(Action.READ, accessPolicies) {
                    val bruker = call.bruker<Bruker<*>>()
                    val identitetsnummer = bruker.hentSluttbrukerIdentitet()
                    val response = bekreftelseService.finnTilgjengeligBekreftelser(identitetsnummer)
                    call.respond(HttpStatusCode.OK, response)
                }
            }

            post<TilgjengeligeBekreftelserRequest>("/tilgjengelige-bekreftelser") { request ->
                val accessPolicies = authorizationService.accessPolicies(request.identitetsnummer?.asIdentitetsnummer())
                autorisering(Action.READ, accessPolicies) {
                    val bruker = call.bruker<Bruker<*>>()
                    val identitetsnummer = bruker.hentSluttbrukerIdentitet(request.identitetsnummer)
                    val response = bekreftelseService.finnTilgjengeligBekreftelser(identitetsnummer)
                    call.respond(HttpStatusCode.OK, response)
                }
            }

            post<MottaBekreftelseRequest>("/bekreftelse") { request ->
                val accessPolicies = authorizationService.accessPolicies(request.identitetsnummer?.asIdentitetsnummer())
                autorisering(Action.WRITE, accessPolicies) {
                    val bruker = call.bruker<Bruker<*>>()
                    val identitetsnummer = bruker.hentSluttbrukerIdentitet(request.identitetsnummer)
                    bekreftelseService.mottaBekreftelse(bruker, identitetsnummer, request)
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}
