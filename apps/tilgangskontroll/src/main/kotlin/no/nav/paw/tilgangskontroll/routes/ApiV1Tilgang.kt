package no.nav.paw.tilgangskontroll.routes

import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.paw.tilgangskontroll.TilgangsTjenesteForAnsatte
import no.nav.paw.tilgangskontroll.api.models.TilgangskontrollRequestV1
import no.nav.paw.tilgangskontroll.api.models.TilgangskontrollResponseV1
import no.nav.paw.tilgangskontroll.api.validering.valider

fun Route.apiV1Tilgang(service: TilgangsTjenesteForAnsatte) {
    post("/api/v1/tilgang") {
        val request: TilgangskontrollRequestV1 = call.receive<TilgangskontrollRequestV1>()
        val validertTilgangskontrollRequest = request.valider()
        val harTilgang = service.harAnsattTilgangTilPerson(
            navIdent = validertTilgangskontrollRequest.navAnsatt,
            identitetsnummer = validertTilgangskontrollRequest.person,
            tilgang = validertTilgangskontrollRequest.tilgang
        )
        call.respond(TilgangskontrollResponseV1(harTilgang = harTilgang))
    }
}