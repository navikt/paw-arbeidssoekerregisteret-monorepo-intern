package no.nav.paw.arbeidssokerregisteret.routes

import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.paw.arbeidssoekerregisteret.api.opplysningermottatt.models.OpplysningerRequest
import no.nav.paw.arbeidssokerregisteret.application.OpplysningerRequestHandler
import no.nav.paw.arbeidssokerregisteret.requestScope
import no.nav.paw.arbeidssokerregisteret.utils.logger

const val opplysningerAPI = "/api/v1/arbeidssoker"
const val opplysninger = "/opplysninger"

fun Route.arbeidssokerRoutes(
    opplysningerRequestHandler: OpplysningerRequestHandler
) {
    route(opplysningerAPI) {
        route(opplysninger) {
            // Registrerer eller oppdaterer brukers opplysninger
            post<OpplysningerRequest> { request ->
                val resultat = opplysningerRequestHandler.opprettBrukeropplysninger(
                    requestScope = requestScope(),
                    opplysningerRequest = request
                )
                logger.debug("Oppdateringsresultat: {}", resultat)
                respondWith(resultat)
            }
        }
    }
}
