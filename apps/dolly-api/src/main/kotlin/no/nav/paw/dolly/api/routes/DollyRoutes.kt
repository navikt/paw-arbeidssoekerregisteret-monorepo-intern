package no.nav.paw.dolly.api.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.paw.dolly.api.models.ArbeidssoekerregistreringRequest
import no.nav.paw.dolly.api.services.DollyService
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
        }
    }
}
