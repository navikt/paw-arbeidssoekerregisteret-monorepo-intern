package no.nav.paw.dolly.api.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.paw.dolly.api.models.ArbeidssoekerregistreringRequest
import no.nav.paw.dolly.api.models.asIdentitetsnummer
import no.nav.paw.dolly.api.models.asTypeRequest
import no.nav.paw.dolly.api.services.DollyService
import no.nav.paw.dolly.api.services.EnumTypeData
import no.nav.paw.security.authentication.interceptor.autentisering
import no.nav.paw.security.authentication.model.AzureAd

fun Route.dollyRoutes(
    dollyService: DollyService
) {
    route("/api/v1") {
        autentisering(AzureAd) {
            post<ArbeidssoekerregistreringRequest>("/arbeidssoekerregistrering") { request ->
                dollyService.registrerArbeidssoeker(request)
                call.response.status(HttpStatusCode.Accepted)
            }

            get("/arbeidssoekerregistrering/{identitetsnummer}") {
                val identitetsnummer = call.parameters["identitetsnummer"].asIdentitetsnummer()
                val arbeidssoeker = dollyService.hentArbeidssoekerregistrering(identitetsnummer)
                if (arbeidssoeker == null) {
                    call.response.status(HttpStatusCode.NotFound)
                    return@get
                }
                call.respond(arbeidssoeker)
            }

            delete("/arbeidssoekerregistrering/{identitetsnummer}") {
                val identitetsnummer = call.parameters["identitetsnummer"].asIdentitetsnummer()
                dollyService.avsluttArbeidssoekerperiode(identitetsnummer)
                call.response.status(HttpStatusCode.NoContent)
            }

            get("/typer/{type}") {
                val type = call.parameters["type"]?.asTypeRequest() ?: return@get
                val response = EnumTypeData.hentEnumTypeResponse(type)
                call.respond(response)
            }
        }
    }
}


