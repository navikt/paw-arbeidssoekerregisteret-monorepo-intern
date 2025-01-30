package no.nav.paw.dolly.api.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
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

            get("/arbeidssoekerregistrering/{identitetsnummer}") {
                val identitetsnummer = call.parameters["identitetsnummer"] ?: throw BadRequestException("Mangler identitetsnummer")
                if (!gyldigIdentitetsnummer(identitetsnummer)) {
                    throw BadRequestException("Identitetsnummer må bestå av 11 sifre")
                }
                val arbeidssoeker = dollyService.hentArbeidssoeker(identitetsnummer)
                call.respond(arbeidssoeker)
            }

            delete("/arbeidssoekerregistrering/{identitetsnummer}") {
                val identitetsnummer = call.parameters["identitetsnummer"] ?: throw BadRequestException("Mangler identitetsnummer")
                if (!gyldigIdentitetsnummer(identitetsnummer)) {
                    throw BadRequestException("Identitetsnummer må bestå av 11 sifre")
                }
                dollyService.avsluttArbeidssoekerperiode(identitetsnummer)
                call.response.status(HttpStatusCode.NoContent)
            }

            get("/typer") {
                call.respond(dollyService.hentEnumMap(null))
            }

            get("/typer/{type}") {
                val type = call.parameters["type"] ?: throw BadRequestException("Mangler type")
                call.respond(dollyService.hentEnumMap(type))
            }
        }
    }
}

fun gyldigIdentitetsnummer(identitetsnummer: String): Boolean = identitetsnummer.matches(Regex("^\\d{11}$"))
