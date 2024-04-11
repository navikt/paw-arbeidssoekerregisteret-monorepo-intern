package no.nav.paw.arbeidssokerregisteret.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.*
import no.nav.paw.arbeidssokerregisteret.application.*
import no.nav.paw.arbeidssokerregisteret.domain.Feilkode
import no.nav.paw.arbeidssokerregisteret.domain.http.*

context(PipelineContext<Unit, ApplicationCall>)
suspend fun respondWith(resultat: ValidationResult) =
    when (resultat) {
        is ValidationErrorResult -> call.respond(
            HttpStatusCode.BadRequest, Feil(
                melding = "felter=${resultat.fields} => ${resultat.message}",
                feilKode = Feilkode.FEIL_VED_LESING_AV_FORESPORSEL
            )
        )
        ValidationResultOk -> call.response.status(HttpStatusCode.OK)
    }
