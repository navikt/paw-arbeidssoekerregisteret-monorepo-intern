package no.nav.paw.arbeidssokerregisteret.routes

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
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
            post {
                val opplysningerRequest = call.receive<OpplysningerRequest>()
                val resultat =
                    with(requestScope()) {
                        opplysningerRequestHandler.opprettBrukeropplysninger(opplysningerRequest)
                    }
                logger.debug("Oppdateringsresultat: {}", resultat)
                respondWith(resultat)
            }
        }
    }
}
