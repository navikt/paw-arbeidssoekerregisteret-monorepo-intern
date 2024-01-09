package no.nav.paw.arbeidssokerregisteret.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.*
import no.nav.paw.arbeidssokerregisteret.domain.*
import no.nav.paw.arbeidssokerregisteret.domain.Feilkode
import no.nav.paw.arbeidssokerregisteret.domain.http.Feil

context(PipelineContext<Unit, ApplicationCall>)
suspend fun respondWith(resultat: Resultat) {
    when (resultat) {
        is Avvist -> call.respond(HttpStatusCode.Forbidden, Feil(resultat.melding, Feilkode.AVVIST))
        is IkkeTilgang -> call.respond(HttpStatusCode.Forbidden, Feil(resultat.melding, Feilkode.IKKE_TILGANG_TIL_BRUKER))
        is OK -> call.respond(HttpStatusCode.OK)
        is Uavklart -> call.respond(HttpStatusCode.Forbidden, Feil(resultat.melding, Feilkode.KUNNE_IKKE_AVGJOERE_OM_BRUKER_KAN_REGISTRERES))
    }
}
