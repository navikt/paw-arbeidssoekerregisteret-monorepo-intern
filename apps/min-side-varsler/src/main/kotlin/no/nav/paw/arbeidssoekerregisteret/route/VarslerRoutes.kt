package no.nav.paw.arbeidssoekerregisteret.route

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.paw.arbeidssoekerregisteret.service.VarselService
import no.nav.paw.arbeidssoekerregisteret.utils.getPaging
import no.nav.paw.arbeidssoekerregisteret.utils.pathVarselId
import no.nav.paw.arbeidssoekerregisteret.utils.queryPeriodeId
import no.nav.paw.security.authentication.model.AzureAd
import no.nav.paw.security.authentication.plugin.autentisering

fun Route.varselRoutes(
    varselService: VarselService
) {
    route("/api/v1/varsler") {
        autentisering(AzureAd) {
            get("/{varselId}") {
                val varselId = call.pathVarselId()
                val response = varselService.hentVarsel(varselId)
                call.respond(HttpStatusCode.OK, response)
            }

            get {
                val paging = call.getPaging()
                val periodeId = call.queryPeriodeId()
                val response = varselService.finnVarsler(periodeId, paging)
                call.respond(HttpStatusCode.OK, response)
            }
        }
    }
}