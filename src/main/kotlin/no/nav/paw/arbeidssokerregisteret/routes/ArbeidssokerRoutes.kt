package no.nav.paw.arbeidssokerregisteret.routes

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import no.nav.paw.arbeidssoekerregisteret.api.opplysningermottatt.models.OpplysningerRequest
import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.ApiV1ArbeidssokerKanStartePeriodePutRequest
import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.ApiV1ArbeidssokerPeriodePutRequest
import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.ApiV1ArbeidssokerPeriodePutRequest.PeriodeTilstand
import no.nav.paw.arbeidssokerregisteret.api.extensions.getId
import no.nav.paw.arbeidssokerregisteret.application.*
import no.nav.paw.arbeidssokerregisteret.requestScope
import no.nav.paw.arbeidssokerregisteret.utils.logger
import java.util.*

fun Route.arbeidssokerRoutes(
    startStoppRequestHandler: StartStoppRequestHandler,
    opplysningerRequestHandler: OpplysningerRequestHandler
) {
    route("/api/v1/arbeidssoker") {
        route("/kanStartePeriode") {
            // Sjekker om bruker kan registreres som arbeidssøker
            put<ApiV1ArbeidssokerKanStartePeriodePutRequest> { request ->
                val uuid = UUID.randomUUID().toString()
                val resultat = with(requestScope()) {
                    startStoppRequestHandler.kanRegistreresSomArbeidssoker(request.getId())
                }
                logger.debug("Resultat av 'kan-starte': {}", resultat)
                respondWith(resultat)
            }
        }

        route("/periode") {
            // Registrerer bruker som arbeidssøker
            put<ApiV1ArbeidssokerPeriodePutRequest> { startStoppRequest ->
                val resultat = with(requestScope()) {
                    when (startStoppRequest.periodeTilstand) {
                        PeriodeTilstand.STARTET ->
                            startStoppRequestHandler.startArbeidssokerperiode(
                                identitetsnummer = startStoppRequest.getId(),
                                erForhaandsGodkjentAvVeileder = startStoppRequest.registreringForhaandsGodkjentAvAnsatt ?: false
                            )
                        PeriodeTilstand.STOPPET ->
                            startStoppRequestHandler.avsluttArbeidssokerperiode(startStoppRequest.getId())
                    }
                }
                logger.debug("Registreringsresultat: {}", resultat)
                when (resultat) {
                    is TilgangskontrollResultat -> respondWith(resultat)
                    is EndeligResultat -> respondWith(resultat)
                }
            }
        }
        route("/opplysninger") {
            // Registrerer eller oppdaterer brukers opplysninger
            post {
                val opplysningerRequest = call.receive<OpplysningerRequest>()
                val resultat =
                    with(requestScope()) {
                        opplysningerRequestHandler.opprettBrukeropplysninger(opplysningerRequest)
                    }
                logger.debug("Oppdateringsresultat: {}", resultat)
                when (resultat) {
                    is Left -> ikkeTilgangTilResponse(resultat.value)
                    is Right -> respondWith(resultat.value)
                }
            }
        }
    }
}
