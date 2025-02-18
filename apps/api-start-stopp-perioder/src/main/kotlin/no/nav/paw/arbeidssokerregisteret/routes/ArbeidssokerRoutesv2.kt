package no.nav.paw.arbeidssokerregisteret.routes

import io.ktor.server.routing.Route
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.ApiV2ArbeidssokerKanStartePeriodePutRequest
import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.ApiV2ArbeidssokerPeriodePutRequest
import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.ApiV2ArbeidssokerPeriodePutRequest.PeriodeTilstand
import no.nav.paw.arbeidssokerregisteret.api.extensions.getId
import no.nav.paw.arbeidssokerregisteret.application.StartStoppRequestHandler
import no.nav.paw.arbeidssokerregisteret.application.feilretting
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
            put<ApiV2ArbeidssokerKanStartePeriodePutRequest> { request ->
                val resultat = startStoppRequestHandler.kanRegistreresSomArbeidssoker(requestScope(), request.getId())
                logger.debug("Resultat av 'kan-starte': {}", resultat)
                respondWithV2(resultat)
            }
        }

        route(periodeV2) {
            // Registrerer bruker som arbeidssøker
            put<ApiV2ArbeidssokerPeriodePutRequest> { request ->
                val resultat = when (val tilstand = request.periodeTilstand) {
                    PeriodeTilstand.STARTET ->
                        startStoppRequestHandler.startArbeidssokerperiode(
                            requestScope = requestScope(),
                            identitetsnummer = request.getId(),
                            erForhaandsGodkjentAvVeileder =
                                request.registreringForhaandsGodkjentAvAnsatt ?: false,
                            feilretting = feilretting(tilstand, request.feilretting)
                        )

                    PeriodeTilstand.STOPPET ->
                        startStoppRequestHandler.avsluttArbeidssokerperiode(
                            requestScope = requestScope(),
                            identitetsnummer = request.getId(),
                            feilretting = feilretting(tilstand, request.feilretting)
                        )
                }
                logger.debug("Registreringsresultat: {}", resultat)
                respondWithV2(resultat)
            }
        }
    }
}
