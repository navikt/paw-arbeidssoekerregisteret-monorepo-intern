package no.nav.paw.arbeidssokerregisteret.routes

import io.ktor.server.routing.*
import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.ApiV1ArbeidssokerKanStartePeriodePutRequest
import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.ApiV1ArbeidssokerPeriodePutRequest
import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.ApiV1ArbeidssokerPeriodePutRequest.PeriodeTilstand
import no.nav.paw.arbeidssokerregisteret.api.extensions.getId
import no.nav.paw.arbeidssokerregisteret.application.StartStoppRequestHandler
import no.nav.paw.arbeidssokerregisteret.requestScope
import no.nav.paw.arbeidssokerregisteret.utils.logger

const val startStopApiV2 = "/api/v2/arbeidssoker"
const val kanStarteV2 = "/kanStartePeriode"
const val periodeV2 = "/periode"

fun Route.arbeidssokerRoutesV2(
    startStoppRequestHandler: StartStoppRequestHandler
) {
    route(startStopApiV2) {
        route(kanStarteV2) {
            // Sjekker om bruker kan registreres som arbeidssøker
            put<ApiV1ArbeidssokerKanStartePeriodePutRequest> { request ->
                val resultat = with(requestScope()) {
                    startStoppRequestHandler.kanRegistreresSomArbeidssoker(request.getId())
                }
                logger.debug("Resultat av 'kan-starte': {}", resultat)
                respondWithV2(resultat)
            }
        }

        route(periodeV2) {
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
                respondWithV2(resultat)
            }
        }
    }
}
