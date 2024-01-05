package no.nav.paw.arbeidssokerregisteret.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.*
import no.nav.paw.arbeidssokerregisteret.domain.*
import no.nav.paw.arbeidssokerregisteret.domain.http.Error
import no.nav.paw.arbeidssokerregisteret.plugins.ErrorCode

context(PipelineContext<Unit, ApplicationCall>)
suspend fun respondWith(resultat: Resultat) {
    when (resultat) {
        is Avvist -> call.respond(HttpStatusCode.Forbidden, Error(resultat.melding, ErrorCode.FEIL))
        is IkkeTilgang -> call.respond(HttpStatusCode.Forbidden, Error(resultat.melding, ErrorCode.FEIL))
        is OK -> call.respond(HttpStatusCode.OK)
        is Uavklart -> call.respond(HttpStatusCode.Forbidden, Error(resultat.melding, ErrorCode.FEIL))
    }
}
