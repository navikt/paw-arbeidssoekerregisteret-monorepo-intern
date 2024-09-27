package no.nav.paw.arbeidssokerregisteret.routes

import arrow.core.Either
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.*
import no.nav.paw.arbeidssoekerregisteret.api.opplysningermottatt.models.Feil

suspend fun PipelineContext<Unit, ApplicationCall>.respondWith(resultat: Either<Feil, Unit>) =
    when (resultat) {
        is Either.Left -> call.respond(resultat.value.feilKode.httpStatusCode(), )
        is Either.Right -> call.respond(HttpStatusCode.Accepted)
    }

fun Feil.FeilKode.httpStatusCode(): HttpStatusCode = when (this) {
    Feil.FeilKode.UKJENT_FEIL -> HttpStatusCode.InternalServerError
    Feil.FeilKode.UVENTET_FEIL_MOT_EKSTERN_TJENESTE -> HttpStatusCode.FailedDependency
    Feil.FeilKode.FEIL_VED_LESING_AV_FORESPORSEL -> HttpStatusCode.BadRequest
    Feil.FeilKode.IKKE_TILGANG -> HttpStatusCode.Forbidden
}
